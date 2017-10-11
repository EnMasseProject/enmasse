/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.artemis.sasl_delegation;

import com.sun.security.auth.UserPrincipal;
import org.apache.activemq.artemis.spi.core.security.jaas.RolePrincipal;
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
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import static org.apache.qpid.proton.engine.Sasl.SaslState.PN_SASL_FAIL;
import static org.apache.qpid.proton.engine.Sasl.SaslState.PN_SASL_IDLE;
import static org.apache.qpid.proton.engine.Sasl.SaslState.PN_SASL_PASS;
import static org.apache.qpid.proton.engine.Sasl.SaslState.PN_SASL_STEP;

public class SaslDelegatingLogin implements LoginModule {

    private static final Logger LOG = Logger.getLogger(SaslDelegatingLogin.class);

    private static final String[] TLS_PROTOCOL_PREFERENCES = new String[]{"TLSv1.2", "TLSv1.1", "TLS", "TLSv1"};
    public static final Symbol AUTHENTICATED_IDENTITY = Symbol.valueOf("authenticated-identity");

    private final Set<Principal> principals = new HashSet<>();
    private final Set<String> roles = new HashSet<>();

    private String host = "localhost";
    private int port = 5672;

    private Subject subject;
    private CallbackHandler callbackHandler;
    private boolean loginSucceeded;
    private String user;
    private Transport transport;
    private Connection connection;
    private Sasl sasl;
    private String container;
    private String saslHostname;
    private SaslMechanismFactory[] saslFactories;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;
    private boolean useTls;
    private String trustStorePath;
    private SSLContext sslContext;
    private char[] trustStorePassword;

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

        if(useTls) {
            try {
                this.sslContext = createSSLContext(options);
            } catch (GeneralSecurityException | IOException e) {
                LOG.error("Unable to create SSL context", e);
            }
        }

        this.options = options;
        loginSucceeded = false;
        transport = Proton.transport();
        connection = Proton.connection();
        transport.bind(connection);
        sasl = transport.sasl();
        sasl.client();
        saslFactories = new SaslMechanismFactory[] { new PlainSaslMechanismFactory() };
    }


    @Override
    public boolean login() throws LoginException {
        boolean success = false;
        try {
            Socket socket = createSocket();

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            transport.open();

            writeToNetwork(out);

            readFromNetwork(in, () -> sasl.getState() == PN_SASL_IDLE && sasl.getRemoteMechanisms().length == 0);

            sasl.setRemoteHostname(saslHostname);

            SaslMechanism mechanism = null;

            for(SaslMechanismFactory factory : saslFactories) {
                if(Arrays.asList(sasl.getRemoteMechanisms()).contains(factory.getName())) {
                    mechanism = factory.newInstance(callbackHandler, sharedState, options);
                    if(mechanism != null) {
                        sasl.setMechanisms(factory.getName());
                        byte[] initialResponse = mechanism.getResponse(null);
                        if(initialResponse != null && initialResponse.length != 0) {
                            sasl.send(initialResponse, 0, initialResponse.length);
                        }
                        break;
                    }
                }
            }

            if(mechanism == null) {
                throw new LoginException("Unable to authenticate using SASL delegation, no supported mechanisms");
            }

            writeToNetwork(out);

            do {
                readFromNetwork(in, () -> !(EnumSet.of(PN_SASL_PASS, PN_SASL_FAIL).contains(sasl.getState())
                    || (sasl.getState() == PN_SASL_STEP  && sasl.pending() > 0)));
                if(sasl.pending() > 0) {
                    byte[] challenge = new byte[sasl.pending()];
                    byte[] response = mechanism.getResponse(challenge);
                    if(sasl.getState() == PN_SASL_STEP) {
                        sasl.send(response, 0, response.length);
                        writeToNetwork(out);
                    }
                }

            } while (sasl.getState() == PN_SASL_STEP);

            if(sasl.getState() == PN_SASL_PASS && mechanism.completedSuccessfully()) {
                connection.setHostname(saslHostname);
                connection.setContainer(container);
                connection.open();
                writeToNetwork(out);
                readFromNetwork(in, () -> connection.getRemoteState() == EndpointState.UNINITIALIZED);

                final Map<Symbol, Object> remoteProperties = connection.getRemoteProperties();
                if(remoteProperties != null
                    && remoteProperties.get(AUTHENTICATED_IDENTITY) instanceof Map) {
                    Map identity = (Map) remoteProperties.get(AUTHENTICATED_IDENTITY);
                    if(identity.containsKey("preferred_username")) {
                        user = String.valueOf(identity.get("preferred_username")).trim();
                    } else {
                        user = String.valueOf(identity.get("sub")).trim();
                    }
                    if(remoteProperties.get(Symbol.valueOf("groups")) instanceof List) {
                        roles.addAll((List<String>)remoteProperties.get(Symbol.valueOf("groups")));
                    }
                }
                success = true;
            } else {
                // login failure
                LOG.debug("Login failed");
            }

            connection.close();
            transport.close();
            socket.close();

        } catch (IOException e) {
            final LoginException loginException = new LoginException("Exception attempting to authenticate using SASL delegation");
            loginException.initCause(e);
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

    private void readFromNetwork(InputStream in, BooleanSupplier test) throws IOException, LoginException {
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

    private void writeToNetwork(OutputStream out) throws IOException {
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

    private SSLContext createSSLContext(Map<String, ?> options) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, KeyManagementException {
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



    // TODO - remove
    public static void main(String[] args) throws LoginException {
        SaslDelegatingLogin login = new SaslDelegatingLogin();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for(Callback callback : callbacks) {
                    if(callback instanceof NameCallback) {
                        ((NameCallback)callback).setName("guest");
                    } else if(callback instanceof PasswordCallback) {
                        ((PasswordCallback)callback).setPassword("guest".toCharArray());
                    }
                }
            }
        };
        Map<String, Object> sharedState = new HashMap<>();
        Map<String, Object> options = new HashMap<>();
        options.put("use_tls", "true");
        options.put("truststore_path", "/Users/rob/testTrustStore.jks");
        options.put("port", "5671");
        login.initialize(subject, callbackHandler, sharedState, options);
        login.login();
        login.commit();
    }

}
