/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api.bind;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.osb.api.EmptyResponse;
import io.enmasse.api.common.Exceptions;
import io.enmasse.osb.api.OSBServiceBase;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressSpaceApi;

import javax.net.ssl.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

@Path(OSBServiceBase.BASE_URI + "/service_instances/{instanceId}/service_bindings/{bindingId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBBindingService extends OSBServiceBase {


    private final String keycloakUrl;
    private final String keycloakAdminUser;
    private final String keycloakAdminPassword;
    private final Random random = new SecureRandom();

    public OSBBindingService(AddressSpaceApi addressSpaceApi, AuthApi authApi, SchemaProvider schemaProvider, String keycloakUrl, String keycloakAdminUser, String keycloakAdminPassword) {
        super(addressSpaceApi, authApi, schemaProvider);
        this.keycloakUrl = keycloakUrl;
        this.keycloakAdminUser = keycloakAdminUser;
        this.keycloakAdminPassword = keycloakAdminPassword;
    }

    @PUT
    public Response bindServiceInstance(@Context SecurityContext securityContext, @PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId, BindRequest bindRequest) {
        log.info("Received bind request for instance {}, binding {} (service id {}, plan id {})",
                instanceId, bindingId, bindRequest.getServiceId(), bindRequest.getPlanId());
        verifyAuthorized(securityContext, ResourceVerb.get);
        AddressSpace addressSpace = findAddressSpaceByInstanceId(instanceId)
                .orElseThrow(() -> Exceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        Map<String, String> parameters = bindRequest.getParameters();

        List<String> allGroups = new ArrayList<>();
        allGroups.addAll(getGroups(parameters.get("sendAddresses"), "send"));
        allGroups.addAll(getGroups(parameters.get("receiveAddresses"), "recv"));
        if(parameters.containsKey("consoleAccess")
                && Boolean.valueOf(parameters.get("consoleAccess"))) {
            allGroups.add("monitor");
        }

        if(parameters.containsKey("consoleAdmin")
                && Boolean.valueOf(parameters.get("consoleAdmin"))) {
            allGroups.add("manage");
        }

        try {

            List<String> groupIds = ensureAllGroups(keycloakAdminUser, keycloakAdminPassword, keycloakUrl, addressSpace, allGroups);

            String accessToken = getAccessToken(keycloakAdminUser, keycloakAdminPassword, keycloakUrl);

            URL url = new URL(keycloakUrl + "/admin/realms/"+addressSpace.getName()+"/users");

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setSSLSocketFactory(getSslSocketFactoryForAuthService());
            conn.setHostnameVerifier((s, sslSession) -> true);
            conn.setDoOutput( true );
            conn.setDoInput( true );
            conn.setInstanceFollowRedirects( false );
            conn.setRequestMethod( "POST" );
            conn.setRequestProperty( "Content-Type", "application/json");
            conn.addRequestProperty( "Accept", "application/json");
            conn.setRequestProperty( "charset", "utf-8");
            conn.setRequestProperty( "Authorization", "bearer " + accessToken );
            conn.setUseCaches( false );

            String username = "user-" + bindingId;

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String,Object> userRep = new LinkedHashMap<>();
            userRep.put("username", username);
            userRep.put("enabled", true);
            try(OutputStream o = conn.getOutputStream()) {
                objectMapper.writeValue(o, userRep);
            }
            int responseCode = conn.getResponseCode();
            String responseMsg = conn.getResponseMessage();
            log.info("Create user request: response - " + responseCode + " : " + responseMsg);

            String userId = getUserId(keycloakAdminUser, keycloakAdminPassword, keycloakUrl, addressSpace, username);

            byte[] passwordBytes = new byte[32];
            this.random.nextBytes(passwordBytes);
            String password = Base64.getEncoder().encodeToString(passwordBytes);
            resetPassword(keycloakAdminUser, keycloakAdminPassword, keycloakUrl, addressSpace, userId, password);

            createGroupMapping(keycloakAdminUser, keycloakAdminPassword, keycloakUrl, addressSpace, userId, groupIds);

            Map<String,String> credentials = new LinkedHashMap<>();
            credentials.put("username",username);
            credentials.put("password", password);
            if ((parameters.containsKey("consoleAccess") && Boolean.valueOf(parameters.get("consoleAccess"))) ||
               (parameters.containsKey("consoleAdmin") && Boolean.valueOf(parameters.get("consoleADmin")))) {
                addressSpace.getEndpoints().stream().filter(e -> e.getName().equals("console")).findFirst().ifPresent(e -> {
                    e.getHost().ifPresent(h-> credentials.put("console", "https://" + h));
                });
            }
            addressSpace.getEndpoints().stream().filter(e -> e.getName().equals("messaging")).findFirst().ifPresent(e -> {
                credentials.put("host", String.format("%s.%s.svc", e.getService(), addressSpace.getNamespace()));
                credentials.put("port", "5671");
                e.getCertSpec().ifPresent(certSpec -> {
                    String cert = getAuthApi().getCert(certSpec.getSecretName(), addressSpace.getNamespace());
                    credentials.put("certificate.pem", cert);
                    /*try {
                        StringWriter keyStoreAsString = new StringWriter();
                        KeyStore keyStore = convertCertToKeyStore(cert);
                        keyStore.store(new OutputStream() {
                            @Override
                            public void write(int i) throws IOException {
                                keyStoreAsString.write(i);
                            }
                        }, new char[0]);
                        credentials.put("keystore.jks", keyStoreAsString.toString());
                    } catch (IOException | GeneralSecurityException e1) {
                        // ignore
                    }
*/
                });
            });
            return Response.status(Response.Status.CREATED).entity(new BindResponse(credentials)).build();

        } catch (IOException | GeneralSecurityException e) {
            log.error("Oops", e);
            throw new InternalServerErrorException("Exception interacting with auth service", e);
        }



 // TODO: return 200 OK, when binding already exists

    }

    private void resetPassword(String keycloakAdminUser, String keycloakAdminPassword, String host, AddressSpace addressSpace, String userId, String password) throws IOException, GeneralSecurityException  {
        String accessToken = getAccessToken(keycloakAdminUser, keycloakAdminPassword, host);

        URL url = new URL(host + "/admin/realms/"+addressSpace.getName()+"/users/"+userId+"/reset-password");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setSSLSocketFactory(getSslSocketFactoryForAuthService());
        conn.setHostnameVerifier((s, sslSession) -> true);
        conn.setInstanceFollowRedirects( false );
        conn.setRequestMethod( "PUT" );
        conn.setRequestProperty( "Content-Type", "application/json");
        conn.addRequestProperty( "Accept", "application/json");
        conn.setRequestProperty( "charset", "utf-8");
        conn.setRequestProperty( "Authorization", "bearer " + accessToken );
        conn.setUseCaches( false );

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> credential = new LinkedHashMap<>();
        credential.put("type","password");
        credential.put("value", password);
        credential.put("temporary", false);
        try(OutputStream o = conn.getOutputStream()) {
            objectMapper.writeValue(o, credential);
        }
        int responseCode = conn.getResponseCode();
        String responseMsg = conn.getResponseMessage();
        log.info("User password reset: response - " + responseCode + " : " + responseMsg);
    }

    private void createGroupMapping(String keycloakAdminUser, String keycloakAdminPassword, String host, AddressSpace addressSpace, String userId, List<String> groupIds) throws IOException, GeneralSecurityException  {
        String accessToken = getAccessToken(keycloakAdminUser, keycloakAdminPassword, host);

        for(String groupId : groupIds) {

            URL url = new URL(host + "/admin/realms/"+addressSpace.getName()+"/users/"+userId+"/groups/"+groupId);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setSSLSocketFactory(getSslSocketFactoryForAuthService());
            conn.setHostnameVerifier((s, sslSession) -> true);
            conn.setInstanceFollowRedirects( false );
            conn.setRequestMethod( "PUT" );
            conn.setRequestProperty( "Authorization", "bearer " + accessToken );
            conn.setUseCaches( false );
            int responseCode = conn.getResponseCode();
            String responseMsg = conn.getResponseMessage();
            log.info("Create user group mapping request: response - " + responseCode + " : " + responseMsg);

        }

    }

    private String getUserId(String keycloakAdminUser, String keycloakAdminPassword, String host, AddressSpace addressSpace, String username) throws IOException, GeneralSecurityException {
        String accessToken = getAccessToken(keycloakAdminUser, keycloakAdminPassword, host);

        URL url = new URL(host + "/admin/realms/"+addressSpace.getName()+"/users");

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setSSLSocketFactory(getSslSocketFactoryForAuthService());
        conn.setHostnameVerifier((s, sslSession) -> true);
        conn.setInstanceFollowRedirects( false );
        conn.setRequestMethod( "GET" );
        conn.addRequestProperty( "Accept", "application/json");
        conn.setRequestProperty( "charset", "utf-8");
        conn.setRequestProperty( "Authorization", "bearer " + accessToken );
        conn.setUseCaches( false );

        StringBuilder builder = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String,Object>> users = objectMapper.readValue(builder.toString(), List.class);
        return (String) users.stream().filter(user -> username.equals(user.get("username"))).findFirst().orElseThrow(() -> new InternalServerErrorException("Unable to find user id of user")).get("id");
    }

    private List<String> ensureAllGroups(String username, String password, String host, AddressSpace addressSpace, List<String> allGroups) throws IOException, GeneralSecurityException {
        String accessToken = getAccessToken(username, password, host);
        List<String> groupIds = new ArrayList<>(allGroups.size());
        Map<String,String> existingGroupIds = getAllExistingGroups(accessToken, host, addressSpace);
        for(String group : allGroups) {
            if(existingGroupIds.containsKey(group)) {
                groupIds.add(existingGroupIds.get(group));
            } else {
                groupIds.add(createGroup(accessToken, host, addressSpace, group));
            }
        }
        return groupIds;
    }

    private String createGroup(String accessToken, String host, AddressSpace addressSpace, String group) throws IOException, GeneralSecurityException {
        URL url = new URL(host + "/admin/realms/"+addressSpace.getName()+"/groups");

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setSSLSocketFactory(getSslSocketFactoryForAuthService());
        conn.setHostnameVerifier((s, sslSession) -> true);
        conn.setInstanceFollowRedirects( false );
        conn.setRequestMethod( "POST" );
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.addRequestProperty( "Accept", "application/json");
        conn.setRequestProperty( "Content-Type", "application/json");
        conn.setRequestProperty( "charset", "utf-8");
        conn.setRequestProperty( "Authorization", "bearer " + accessToken );
        conn.setUseCaches( false );

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String,Object> groupRep = Collections.singletonMap("name", group);
        try(OutputStream o = conn.getOutputStream()) {
            objectMapper.writeValue(o, groupRep);
        }
        int responseCode = conn.getResponseCode();
        String responseMsg = conn.getResponseMessage();
        log.info("Create group request: response - " + responseCode + " : " + responseMsg);

        return getAllExistingGroups(accessToken, host, addressSpace).get(group);

    }

    private Map<String, String> getAllExistingGroups(String accessToken, String host, AddressSpace addressSpace) throws IOException, GeneralSecurityException {
        URL url = new URL(host + "/admin/realms/"+addressSpace.getName()+"/groups");

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setSSLSocketFactory(getSslSocketFactoryForAuthService());
        conn.setHostnameVerifier((s, sslSession) -> true);
        conn.setInstanceFollowRedirects( false );
        conn.setRequestMethod( "GET" );
        conn.addRequestProperty( "Accept", "application/json");
        conn.setRequestProperty( "charset", "utf-8");
        conn.setRequestProperty( "Authorization", "bearer " + accessToken );
        conn.setUseCaches( false );

        StringBuilder builder = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String,Object>> groups = objectMapper.readValue(builder.toString(), List.class);
        Map<String, String> existingGroups = new HashMap<>();
        for(Map<String,Object> group : groups) {
            existingGroups.put((String)group.get("name"), (String)group.get("id"));
        }
        return existingGroups;
    }

    private Collection<String> getGroups(String addressList, String prefix) {
        Set<String> groups = new HashSet<>();
        if(addressList != null) {
            for(String address : addressList.split(",")) {
                address = address.trim();
                if(address.length()>0) {
                    try {
                        groups.add(prefix+"_"+URLEncoder.encode(address, StandardCharsets.UTF_8.name()));
                    } catch (UnsupportedEncodingException e) {
                        // UTF-8 must always be supported
                    }
                }
            }
        }
        return groups;
    }

    private String getAccessToken(String username, String password, String host) throws IOException, GeneralSecurityException {
        URL url = new URL(host + "/realms/master/protocol/openid-connect/token");

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setSSLSocketFactory(getSslSocketFactoryForAuthService());
        conn.setHostnameVerifier((s, sslSession) -> true);

        String urlParameters  = "client_id=admin-cli" +
                "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8.name()) +
                "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) +
                "&grant_type=password";
        byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
        int    postDataLength = postData.length;
        conn.setDoOutput( true );
        conn.setDoInput( true );
        conn.setInstanceFollowRedirects( false );
        conn.setRequestMethod( "POST" );
        conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
        conn.addRequestProperty( "Accept", "application/json");
        conn.setRequestProperty( "charset", "utf-8");
        conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
        conn.setUseCaches( false );
        try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
            wr.write( postData );
        }

        StringBuilder builder = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String,Object> obj = objectMapper.readValue(builder.toString(), Map.class);
        return (String) obj.get("access_token");
    }

    private SSLSocketFactory getSslSocketFactoryForAuthService() throws IOException, GeneralSecurityException {
        String secretName = "standard-authservice-cert";
        String cert = getAuthApi().getCert(secretName, getAuthApi().getNamespace());
        KeyStore inMemoryKeyStore = convertCertToKeyStore(cert);
        SSLContext sc = SSLContext.getInstance("TLS");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(inMemoryKeyStore);
        sc.init(null, tmf.getTrustManagers(), null);
        return sc.getSocketFactory();
    }

    private KeyStore convertCertToKeyStore(String cert) throws IOException, GeneralSecurityException {
        List<X509Certificate> certs = new ArrayList<>();
        try (InputStream is = new ByteArrayInputStream(cert.getBytes(StandardCharsets.UTF_8))) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                do {
                    certs.add((X509Certificate)cf.generateCertificate(is));
                } while(is.available() != 0);
            } catch (CertificateException e) {
                if(certs.isEmpty()) {
                    throw new InternalServerErrorException("No auth service certificate found in secret", e);
                }
            }
        }

        KeyStore inMemoryKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        inMemoryKeyStore.load(null, null);
        int i = 1;
        for(X509Certificate crt : certs) {
            inMemoryKeyStore.setCertificateEntry(String.valueOf(i++), crt);
        }
        return inMemoryKeyStore;
    }


    @DELETE
    public Response unbindServiceInstance(@Context SecurityContext securityContext, @PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId) {
        log.info("Received unbind request for instance {}, binding {}", instanceId, bindingId);
        verifyAuthorized(securityContext, ResourceVerb.get);
/*
        AddressSpace addressSpace = findAddressSpaceByAddressUuid(instanceId)
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));
*/

        return Response.ok(new EmptyResponse()).build();
    }

}
