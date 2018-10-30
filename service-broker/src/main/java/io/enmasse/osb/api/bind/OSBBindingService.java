/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api.bind;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.osb.api.EmptyResponse;
import io.enmasse.osb.api.OSBServiceBase;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@Path(OSBServiceBase.BASE_URI + "/service_instances/{instanceId}/service_bindings/{bindingId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBBindingService extends OSBServiceBase {

    private final UserApi userApi;
    private final Random random = new SecureRandom();

    public OSBBindingService(AddressSpaceApi addressSpaceApi, AuthApi authApi, SchemaProvider schemaProvider, UserApi userApi) {
        super(addressSpaceApi, authApi, schemaProvider);
        this.userApi = userApi;
    }

    @PUT
    public Response bindServiceInstance(@Context SecurityContext securityContext, @PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId, BindRequest bindRequest) {
        log.info("Received bind request for instance {}, binding {} (service id {}, plan id {})",
                instanceId, bindingId, bindRequest.getServiceId(), bindRequest.getPlanId());
        verifyAuthorized(securityContext, ResourceVerb.get);
        AddressSpace addressSpace = findAddressSpaceByInstanceId(instanceId)
                .orElseThrow(() -> Exceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        Map<String, String> parameters = bindRequest.getParameters();

        String username = "user-" + bindingId;
        String password = generatePassword();

        try {

            createOrReplaceUser(addressSpace, username, password, parameters);

            Map<String, String> credentials = new LinkedHashMap<>();
            credentials.put("username", username);
            credentials.put("password", password);
            if ((parameters.containsKey("consoleAccess") && Boolean.valueOf(parameters.get("consoleAccess"))) ||
               (parameters.containsKey("consoleAdmin") && Boolean.valueOf(parameters.get("consoleAdmin")))) {
                addressSpace.getStatus().getEndpointStatuses().stream().filter(e -> e.getName().startsWith("console")).findFirst().ifPresent(e -> {
                    if (e.getExternalHost() != null) {
                        credentials.put("console", "https://" + e.getExternalHost());
                    }
                });
            }

            for (EndpointSpec endpointSpec : addressSpace.getEndpoints()) {
                if (endpointSpec.getService().startsWith("console")) {
                    continue;
                }
                String prefix = endpointSpec.getName();

                EndpointStatus endpointStatus = null;
                for (EndpointStatus status : addressSpace.getStatus().getEndpointStatuses()) {
                    if (status.getName().equals(endpointSpec.getName())) {
                        endpointStatus = status;
                        break;
                    }
                }
                if (endpointStatus == null) {
                    continue;
                }

                if (parameters.containsKey("externalAccess") && Boolean.valueOf(parameters.get("externalAccess")) && endpointStatus.getExternalHost() != null) {
                    String externalPrefix = "external" + prefix.substring(0, 1).toUpperCase() + prefix.substring(1);
                    credentials.put(externalPrefix + "Host", endpointStatus.getExternalHost());
                    credentials.put(externalPrefix + "Port", String.format("%d", endpointStatus.getExternalPorts().values().iterator().next()));
                }
                credentials.put(prefix + "Host", endpointStatus.getServiceHost());
                for (Map.Entry<String, Integer> servicePort : endpointStatus.getServicePorts().entrySet()) {
                    String portName = servicePort.getKey().substring(0, 1).toUpperCase() + servicePort.getKey().substring(1);
                    credentials.put(prefix + portName + "Port", String.format("%d", servicePort.getValue()));
                }
                endpointSpec.getCertSpec().ifPresent(certSpec -> {
                    String cert = getAuthApi().getCert(certSpec.getSecretName());
                    credentials.put(prefix + "Cert.pem", cert);
                });
            }
            return Response.status(Response.Status.CREATED).entity(new BindResponse(credentials)).build();

        } catch (Exception e) {
            throw new InternalServerErrorException("Exception interacting with auth service", e);
        }

 // TODO: return 200 OK, when binding already exists

    }

    private User createOrReplaceUser(AddressSpace addressSpace, String username, String password, Map<String, String> parameters) throws Exception {
        UserSpec.Builder specBuilder = new UserSpec.Builder();
        specBuilder.setUsername(username);
        specBuilder.setAuthentication(new UserAuthentication.Builder()
                .setType(UserAuthenticationType.password)
                .setPassword(Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)))
                .build());


        List<UserAuthorization> authorizations = new ArrayList<>();

        authorizations.add(new UserAuthorization.Builder()
                .setOperations(Arrays.asList(Operation.send))
                .setAddresses(getAddresses(parameters.get("sendAddresses")))
                .build());

        authorizations.add(new UserAuthorization.Builder()
                .setOperations(Arrays.asList(Operation.recv))
                .setAddresses(getAddresses(parameters.get("receiveAddresses")))
                .build());

        if(parameters.containsKey("consoleAccess")
                && Boolean.valueOf(parameters.get("consoleAccess"))) {
            authorizations.add(new UserAuthorization.Builder()
                    .setOperations(Arrays.asList(Operation.view))
                    .setAddresses(Arrays.asList("*"))
                    .build());
        }

        if(parameters.containsKey("consoleAdmin")
                && Boolean.valueOf(parameters.get("consoleAdmin"))) {
            authorizations.add(new UserAuthorization.Builder()
                    .setOperations(Arrays.asList(Operation.manage))
                    .build());
        }

        specBuilder.setAuthorization(authorizations);

        User user = new User.Builder()
            .setMetadata(new UserMetadata.Builder()
                    .setNamespace(addressSpace.getNamespace())
                    .setName(addressSpace.getName() + "." + username)
                    .build())
                        .setSpec(specBuilder.build())
                        .build();

        String realmName = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
        if (userApi.getUserWithName(realmName, username).isPresent()) {
            userApi.replaceUser(realmName, user);
        } else {
            userApi.createUser(realmName, user);
        }
        return user;
    }

    private static final String PASSWORD_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final int passwordLength = 32;

    private String generatePassword() {
        StringBuilder builder = new StringBuilder();
        int length = passwordLength;
        while (length-- != 0) {
            int character = (int)(random.nextDouble()*PASSWORD_CHARACTERS.length());
            builder.append(PASSWORD_CHARACTERS.charAt(character));
        }
        return builder.toString();
    }

    private Collection<String> getAddresses(String addressList) {
        Set<String> groups = new HashSet<>();
        if(addressList != null) {
            for(String address : addressList.split(",")) {
                address = address.trim();
                if(address.length()>0) {
                    groups.add(address);
                }
            }
        }
        return groups;
    }

    @DELETE
    public Response unbindServiceInstance(@Context SecurityContext securityContext, @PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId) {
        log.info("Received unbind request for instance {}, binding {}", instanceId, bindingId);
        verifyAuthorized(securityContext, ResourceVerb.get);

        AddressSpace addressSpace = findAddressSpaceByInstanceId(instanceId)
            .orElseThrow(() -> Exceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        try {
            String username = "user-" + bindingId;

            if(deleteUser(addressSpace, username)) {
                return Response.ok(new EmptyResponse()).build();
            } else {
                return Response.status(Response.Status.GONE).build();
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Exception interacting with auth service", e);
        }

    }

    private boolean deleteUser(AddressSpace addressSpace, String username) throws Exception {

        String realmName = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
        Optional<User> user = userApi.getUserWithName(realmName, username);
        if (user.isPresent()) {
            userApi.deleteUser(realmName, user.get());
            return true;
        } else {
            return false;
        }

    }

}
