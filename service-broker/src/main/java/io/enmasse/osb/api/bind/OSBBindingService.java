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
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.osb.UserApi;
import io.enmasse.osb.api.EmptyResponse;
import io.enmasse.osb.api.OSBServiceBase;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationBuilder;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserAuthorization;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.enmasse.user.model.v1.UserBuilder;
import io.enmasse.user.model.v1.UserSpecBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

            for (EndpointSpec endpointSpec : addressSpace.getSpec().getEndpoints()) {
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

                String externalPrefix = "external" + prefix.substring(0, 1).toUpperCase() + prefix.substring(1);
                credentials.put(externalPrefix + "Host", endpointStatus.getExternalHost());
                credentials.put(externalPrefix + "Port", String.format("%d", endpointStatus.getExternalPorts().values().iterator().next()));
                credentials.put(prefix + "Host", endpointStatus.getServiceHost());
                for (Map.Entry<String, Integer> servicePort : endpointStatus.getServicePorts().entrySet()) {
                    String portName = servicePort.getKey().substring(0, 1).toUpperCase() + servicePort.getKey().substring(1);
                    credentials.put(prefix + portName + "Port", String.format("%d", servicePort.getValue()));
                }
                if(endpointSpec.getCert() !=null) {
                    String cert = getAuthApi().getCert(endpointSpec.getCert().getSecretName());
                    credentials.put(prefix + "Cert.pem", cert);
                }
            }
            return Response.status(Response.Status.CREATED).entity(new BindResponse(credentials)).build();

        } catch (Exception e) {
            throw new InternalServerErrorException("Exception interacting with auth service", e);
        }

 // TODO: return 200 OK, when binding already exists

    }

    private User createOrReplaceUser(AddressSpace addressSpace, String username, String password, Map<String, String> parameters) throws Exception {
        UserSpecBuilder specBuilder = new UserSpecBuilder();
        specBuilder.withUsername(username);
        specBuilder.withAuthentication(new UserAuthenticationBuilder()
                .withType(UserAuthenticationType.password)
                .withPassword(Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)))
                .build());


        List<UserAuthorization> authorizations = new ArrayList<>();

        authorizations.add(new UserAuthorizationBuilder()
                .withOperations(Arrays.asList(Operation.send))
                .withAddresses(getAddresses(parameters.get("sendAddresses")))
                .build());

        authorizations.add(new UserAuthorizationBuilder()
                .withOperations(Arrays.asList(Operation.recv))
                .withAddresses(getAddresses(parameters.get("receiveAddresses")))
                .build());

        specBuilder.withAuthorization(authorizations);

        User user = new UserBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(addressSpace.getMetadata().getName() + "." + username)
                .endMetadata()

                .withSpec(specBuilder.build())
                .build();

        userApi.createOrReplace(user);
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

    private List<String> getAddresses(String addressList) {
        final Set<String> groups = new HashSet<>();
        if(addressList != null) {
            for(String address : addressList.split(",")) {
                address = address.trim();
                if(address.length()>0) {
                    groups.add(address);
                }
            }
        }
        return new ArrayList<>(groups);
    }

    @DELETE
    public Response unbindServiceInstance(@Context SecurityContext securityContext, @PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId) {
        log.info("Received unbind request for instance {}, binding {}", instanceId, bindingId);
        verifyAuthorized(securityContext, ResourceVerb.get);

        AddressSpace addressSpace = findAddressSpaceByInstanceId(instanceId).orElse(null);
        if (addressSpace == null) {
            return Response.status(Response.Status.GONE).entity("{}").build();
        }

        try {
            String username = "user-" + bindingId;

            if(deleteUser(addressSpace, username)) {
                return Response.ok(new EmptyResponse()).build();
            } else {
                return Response.status(Response.Status.GONE).entity("{}").build();
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Exception interacting with auth service", e);
        }

    }

    private boolean deleteUser(AddressSpace addressSpace, String username) throws Exception {
        return userApi.deleteUser(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName() + "." + username);
    }

}
