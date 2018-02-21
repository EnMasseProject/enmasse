/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.artemis.sasl_delegation;

import org.apache.activemq.artemis.spi.core.security.jaas.CertificateCallback;
import org.apache.activemq.artemis.spi.core.security.jaas.RolePrincipal;
import org.apache.activemq.artemis.spi.core.security.jaas.UserPrincipal;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.TransportResult;
import org.jboss.logging.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.cert.X509Certificate;

import static org.apache.qpid.proton.engine.Sasl.SaslState.PN_SASL_FAIL;
import static org.apache.qpid.proton.engine.Sasl.SaslState.PN_SASL_IDLE;
import static org.apache.qpid.proton.engine.Sasl.SaslState.PN_SASL_PASS;
import static org.apache.qpid.proton.engine.Sasl.SaslState.PN_SASL_STEP;

public class SaslDelegatingLogin implements LoginModule {

    private static final Logger LOG = Logger.getLogger(SaslDelegatingLogin.class);

    private static final String[] TLS_PROTOCOL_PREFERENCES = new String[]{"TLSv1.2", "TLSv1.1", "TLS", "TLSv1"};
    private static final Symbol AUTHENTICATED_IDENTITY = Symbol.valueOf("authenticated-identity");
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String SUB = "sub";
    public static final String PROP_ADDRESS_AUTHZ = "address-authz";
    public static final Symbol CAPABILITY_ADDRESS_AUTHZ = Symbol.valueOf("ADDRESS-AUTHZ");

    private final Set<Principal> principals = new HashSet<>();
    private final Set<String> roles = new HashSet<>();

    private String host = "localhost";
    private int port = 5672;

    private Subject subject;
    private CallbackHandler callbackHandler;
    private boolean loginSucceeded;
    private String user;
    private String container;
    private String saslHostname;
    private SaslMechanismFactory[] saslFactories;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;
    private boolean useTls;
    private String trustStorePath;
    private SSLContext sslContext;
    private char[] trustStorePassword;
    private Map<String, List<String>> validCertUsers = new HashMap<>();
    private String securitySettings;

    @Override
    public void initialize(Subject subject,
                           CallbackHandler callbackHandler,
                           Map<String, ?> sharedState,
                           Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;

        if(options.containsKey("hostname")) {
            this.host = String.valueOf(options.get("hostname")).trim();
        }
        if(options.containsKey("port")) {
            this.port = Integer.parseInt(String.valueOf(options.get("port")).trim());
        }
        if(options.containsKey("sasl_hostname")) {
            this.saslHostname = String.valueOf(options.get("sasl_hostname")).trim();
        } else {
            this.saslHostname = this.host;
        }
        if(options.containsKey("container_id")) {
            this.container = String.valueOf(options.get("container_id")).trim();
        } else {
            this.container = this.saslHostname;
        }
        if(options.containsKey("use_tls")) {
            this.useTls = Boolean.parseBoolean(String.valueOf(options.get("use_tls")).trim());
        }
        if(options.containsKey("truststore_path")) {
            this.trustStorePath = String.valueOf(options.get("truststore_path")).trim();
        }
        if(options.containsKey("truststore_password")) {
            this.trustStorePassword = String.valueOf(options.get("truststore_password")).trim().toCharArray();
        }
        if(options.containsKey("valid_cert_users")) {
            validCertUsers.clear();
            String[] userDetails = String.valueOf(options.get("valid_cert_users")).trim().split(";");
            for(String userDetail : userDetails) {
                List<String> roles = new ArrayList<>();
                String[] userRoles = userDetail.split(":", 2);
                if(userRoles.length == 2) {
                    for(String role : userRoles[1].split(",")) {
                        roles.add(role.trim());
                    }
                }
                validCertUsers.put(userRoles[0], roles);
            }
        }
        if(options.containsKey("security_settings")) {
            this.securitySettings = String.valueOf(options.get("security_settings")).trim();
        }

        if(useTls) {
            try {
                this.sslContext = createSSLContext();
            } catch (GeneralSecurityException | IOException e) {
                LOG.error("Unable to create SSL context", e);
            }
        }

        this.options = options;
        loginSucceeded = false;
        saslFactories = new SaslMechanismFactory[] { new PlainSaslMechanismFactory() };
    }


    @Override
    public boolean login() throws LoginException {

        boolean success = false;
        try {
            List<X509Certificate> certs = new ArrayList<>();

            if(isAuthenticatedUsingCerts(certs)) {
                success = populateUserAndRolesFromCert(certs.get(0));
            } else {

                Transport transport = Proton.transport();
                Connection connection = Proton.connection();
                transport.bind(connection);
                Sasl sasl = transport.sasl();
                sasl.client();

                Socket socket = createSocket();

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                transport.open();

                // write Headers
                writeToNetwork(connection, out);

                SaslMechanism mechanism = chooseSaslMechanismAndSendInit(connection, in, out);

                performSaslSteps(connection, in, out, mechanism);

                if (isSaslAuthenticated(connection, mechanism)) {
                    performConnectionOpen(connection, in, out);
                    getUserAndRolesFromConnection(connection);
                    success = true;
                } else {
                    LOG.debug("Login failed");
                }

                connection.close();
                transport.close();
                socket.close();
            }

        } catch (IOException | UnsupportedCallbackException | InvalidNameException e) {
            LoginException loginException = new LoginException("Exception attempting to authenticate using SASL delegation");
            loginException.initCause(e);
            LOG.warn(e);
            throw loginException;
        }
        loginSucceeded = success;
        return success;
    }

    @Override
    public boolean commit() throws LoginException {
        boolean result = loginSucceeded;
        try {
            if (result) {
                UserPrincipal userPrincipal = new UserPrincipal(user);
                principals.add(userPrincipal);
                LOG.debugv("Adding user principal for: {0}", user);
                for (String entry : roles) {
                    LOG.debugv("Adding role principal for: {0}", entry);
                    principals.add(new RolePrincipal(entry));
                }

                subject.getPrincipals().addAll(principals);
            }

            clear();

            LOG.debugv("commit, result: {0}", result);

        } catch (RuntimeException | Error  e) {
            LOG.error(e);
            throw e;
        }
        return result;
    }

    @Override
    public boolean abort() throws LoginException {
        clear();

        LOG.debug("abort");
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        clear();
        LOG.debug("logout");
        return true;
    }

    private void clear() {
        user = null;
        principals.clear();
        roles.clear();
        loginSucceeded = false;
    }



    private boolean populateUserAndRolesFromCert(X509Certificate certificate) throws InvalidNameException {
        LdapName ldapName = new LdapName(certificate.getSubjectDN().getName());

        // Find the first CN that maps to a valid user as supplied in the config
        Optional<String> validCN = ldapName.getRdns().stream()
            .filter(rdn -> rdn.getType().equalsIgnoreCase("CN"))
            .map(rdn -> String.valueOf(rdn.getValue()))
            .filter(n -> validCertUsers.containsKey(n)).findFirst();

        if(validCN.isPresent()) {
            String name = validCN.get();
            user = name; roles.addAll(validCertUsers.get(name));
            return true;
        } else {
            return false;
        }
    }

    private boolean isAuthenticatedUsingCerts(List<X509Certificate> certs) throws IOException, UnsupportedCallbackException {
        NameCallback nameHandler = new NameCallback("user:");
        CertificateCallback certificateCallback = new CertificateCallback();
        callbackHandler.handle(new Callback[] { nameHandler, certificateCallback });
        X509Certificate[] certArray = certificateCallback.getCertificates();
        if(certArray != null) {
            certs.addAll(Arrays.asList(certArray));
        }
        return nameHandler.getName() == null && !certs.isEmpty();
    }

    private void getUserAndRolesFromConnection(Connection connection) {
        final Map<Symbol, Object> remoteProperties = connection.getRemoteProperties();
        Symbol[] supportedCapabilities = connection.getRemoteOfferedCapabilities();
        boolean supportsAuthz = supportedCapabilities != null && Arrays.asList(supportedCapabilities).contains(CAPABILITY_ADDRESS_AUTHZ);
        if (remoteProperties != null && remoteProperties.get(AUTHENTICATED_IDENTITY) instanceof Map) {
            Map identity = (Map) remoteProperties.get(AUTHENTICATED_IDENTITY);
            if (identity.containsKey(PREFERRED_USERNAME)) {
                user = String.valueOf(identity.get(PREFERRED_USERNAME)).trim();
            } else {
                user = String.valueOf(identity.get(SUB)).trim();
            }
            roles.add("all");

        }

        if(supportsAuthz) {
            if (remoteProperties != null && remoteProperties.get(Symbol.valueOf(PROP_ADDRESS_AUTHZ)) instanceof Map) {
                Map<String, String[]> authz = (Map<String, String[]>) remoteProperties.get(Symbol.valueOf(PROP_ADDRESS_AUTHZ));
                List<String> groups = new ArrayList<>();
                for(Map.Entry<String, String[]> entry : authz.entrySet()) {
                    for(String permission : entry.getValue()) {
                        groups.add(permission + "_" + entry.getKey());
                    }
                }
                roles.addAll(groups);
                if(this.securitySettings != null) {
                    SaslGroupBasedSecuritySettingsPlugin securitySettingPlugin = SaslGroupBasedSecuritySettingsPlugin.getInstance(this.securitySettings);
                    if(securitySettingPlugin != null) {
                        securitySettingPlugin.addGroups(groups);
                    }
                }
            }

        } else {
            if(this.securitySettings != null) {
                SaslGroupBasedSecuritySettingsPlugin securitySettingPlugin = SaslGroupBasedSecuritySettingsPlugin.getInstance(this.securitySettings);
                if(securitySettingPlugin != null) {
                    LOG.infov("Using sasl delegation for authz, but delegate does offer support, adding admin role to : {0}", user);
                    roles.add("admin");
                }
            }
        }

    }

    private void performConnectionOpen(Connection connection, InputStream in, OutputStream out) throws IOException, LoginException {
        connection.setHostname(saslHostname);
        connection.setContainer(container);
        connection.setDesiredCapabilities(new Symbol[] {CAPABILITY_ADDRESS_AUTHZ});
        connection.open();
        writeToNetwork(connection, out);
        readFromNetwork(connection, in, () -> connection.getRemoteState() == EndpointState.UNINITIALIZED);
    }

    private void performSaslSteps(Connection connection, InputStream in,
                                  OutputStream out,
                                  SaslMechanism mechanism) throws IOException, LoginException {
        Transport transport = connection.getTransport();
        Sasl sasl = transport.sasl();
        do {

            readFromNetwork(connection, in, () ->
                !(EnumSet.of(PN_SASL_PASS, PN_SASL_FAIL).contains(sasl.getState())
                || (sasl.getState() == PN_SASL_STEP && sasl.pending() > 0)));

            if (sasl.pending() > 0) {
                byte[] challenge = new byte[sasl.pending()];
                byte[] response = mechanism.getResponse(challenge);
                if (sasl.getState() == PN_SASL_STEP) {
                    sasl.send(response, 0, response.length);
                    writeToNetwork(connection, out);
                }
            }

        } while (sasl.getState() == PN_SASL_STEP);
    }

    private SaslMechanism chooseSaslMechanismAndSendInit(Connection connection, InputStream in, OutputStream out) throws LoginException, IOException {

        Transport transport = connection.getTransport();
        Sasl sasl = transport.sasl();
        SaslMechanism mechanism = null;
        // read from network until we get a sasl-mechanisms
        readFromNetwork(connection, in, () -> sasl.getState() == PN_SASL_IDLE && sasl.getRemoteMechanisms().length == 0);

        for (SaslMechanismFactory factory : saslFactories) {
            if (Arrays.asList(sasl.getRemoteMechanisms()).contains(factory.getName())) {
                mechanism = factory.newInstance(callbackHandler, sharedState, options);
                if (mechanism != null) {
                    sasl.setRemoteHostname(saslHostname);
                    sasl.setMechanisms(factory.getName());
                    byte[] initialResponse = mechanism.getResponse(null);
                    if (initialResponse != null && initialResponse.length != 0) {
                        sasl.send(initialResponse, 0, initialResponse.length);
                    }
                    break;
                }
            }
        }

        if (mechanism == null) {
            throw new LoginException("Unable to authenticate using SASL delegation, no supported mechanisms");
        }

        writeToNetwork(connection, out);
        return mechanism;
    }

    private boolean isSaslAuthenticated(Connection connection, SaslMechanism mechanism) {
        Transport transport = connection.getTransport();
        Sasl sasl = transport.sasl();
        return sasl.getState() == PN_SASL_PASS && mechanism.completedSuccessfully();
    }

    private Socket createSocket() throws IOException, LoginException {
        if(this.useTls) {
            if(sslContext == null) {
                throw new LoginException("Unable to establish SSL connection due to configuration errors");
            }
            return sslContext.getSocketFactory().createSocket(host, port);
        } else {
            return new Socket(host, port);
        }
    }

    private void readFromNetwork(Connection connection, InputStream in, BooleanSupplier test) throws IOException, LoginException {
        Transport transport = connection.getTransport();
        while(test.getAsBoolean()) {
            ByteBuffer buf = transport.getInputBuffer();
            byte[] tmpBuf = new byte[buf.remaining()];
            int bytesRead = in.read(tmpBuf);
            LOG.tracev("read {0} bytes", bytesRead);
            if (bytesRead == -1) {
                throw new LoginException("Unexpected EOS experienced when authenticating using SASL delegation");
            } else {
                buf.put(tmpBuf, 0, bytesRead);
                TransportResult result = transport.processInput();
                if(!result.isOk()) {
                    LoginException e = new LoginException("Unexpected error when authenticating using SASL delegation");
                    e.initCause(result.getException());
                    throw e;
                }
            }

        }

    }

    private void writeToNetwork(Connection connection, OutputStream out) throws IOException {
        Transport transport = connection.getTransport();
        while(transport.pending() > 0)
        {
            ByteBuffer outputBuf = transport.head();
            final int size = outputBuf.remaining();
            byte[] tmpBuf = new byte[size];
            outputBuf.get(tmpBuf);
            LOG.tracev("writing {0} bytes", size);
            out.write(tmpBuf);
            transport.pop(size);
        }
    }

    private SSLContext createSSLContext() throws GeneralSecurityException, IOException {
        SSLContext sslContext = tryGetSSLContext();
        TrustManager[] trustManagers = null;
        if(this.trustStorePath != null) {
            KeyStore ts = KeyStore.getInstance("JKS");
            try(InputStream in = new FileInputStream(trustStorePath))
            {
                ts.load(in, trustStorePassword);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            trustManagers = tmf.getTrustManagers();
        }
        sslContext.init(null, trustManagers, null);

        return sslContext;
    }

    private static SSLContext tryGetSSLContext() throws NoSuchAlgorithmException {
        return tryGetSSLContext(TLS_PROTOCOL_PREFERENCES);
    }

    private static SSLContext tryGetSSLContext(final String[] protocols) throws NoSuchAlgorithmException {
        for (String protocol : protocols) {
            try {
                return SSLContext.getInstance(protocol);
            } catch (NoSuchAlgorithmException e) {
                // pass and try the next protocol in the list
            }
        }
        throw new NoSuchAlgorithmException(String.format("Could not create SSLContext with one of the requested protocols: %s",
                                                         Arrays.toString(protocols)));
    }

}
