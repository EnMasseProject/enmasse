/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api.bind;

import io.enmasse.address.model.Endpoint;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.osb.api.EmptyResponse;
import io.enmasse.api.common.Exceptions;
import io.enmasse.osb.api.OSBServiceBase;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.osb.keycloak.KeycloakApi;
import io.enmasse.osb.keycloak.KeycloakClient;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.stream.Collectors;

@Path(OSBServiceBase.BASE_URI + "/service_instances/{instanceId}/service_bindings/{bindingId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBBindingService extends OSBServiceBase {


    private final KeycloakApi keycloakApi;
    private final Random random = new SecureRandom();

    public OSBBindingService(AddressSpaceApi addressSpaceApi, AuthApi authApi, SchemaProvider schemaProvider, KeycloakApi keycloakApi) {
        super(addressSpaceApi, authApi, schemaProvider);
        this.keycloakApi = keycloakApi;
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

        try (KeycloakClient keycloakClient = keycloakApi.getInstance()) {

            List<String> groupIds = ensureAllGroups(keycloakClient, addressSpace, allGroups);

            String username = "user-" + bindingId;
            byte[] passwordBytes = new byte[32];
            this.random.nextBytes(passwordBytes);
            String password = Base64.getEncoder().encodeToString(passwordBytes);

            UserRepresentation userRep = keycloakClient.createUser(addressSpace.getName(), username, password);

            createGroupMapping(keycloakClient, addressSpace, userRep.getId(), groupIds);

            Map<String,String> credentials = new LinkedHashMap<>();
            credentials.put("username",username);
            credentials.put("password", password);
            if ((parameters.containsKey("consoleAccess") && Boolean.valueOf(parameters.get("consoleAccess"))) ||
               (parameters.containsKey("consoleAdmin") && Boolean.valueOf(parameters.get("consoleAdmin")))) {
                addressSpace.getEndpoints().stream().filter(e -> e.getName().equals("console")).findFirst().ifPresent(e -> {
                    e.getHost().ifPresent(h-> credentials.put("console", "https://" + h));
                });
            }

            for (Endpoint endpoint : addressSpace.getEndpoints()) {
                if ("console".equals(endpoint.getService())) {
                    continue;
                }
                String prefix = endpoint.getName();
                if (parameters.containsKey("externalAccess") && Boolean.valueOf(parameters.get("externalAccess")) && endpoint.getHost().isPresent()) {
                    endpoint.getHost().ifPresent(host -> {
                        String externalPrefix = "external" + prefix.substring(0, 1).toUpperCase() + prefix.substring(1);
                        credentials.put(externalPrefix + "Host", host);
                        credentials.put(externalPrefix + "Port", String.format("%d", endpoint.getPort()));
                    });
                }
                credentials.put(prefix + "Host", String.format("%s.%s.svc", endpoint.getService(), addressSpace.getNamespace()));
                for (Map.Entry<String, Integer> servicePort : endpoint.getServicePorts().entrySet()) {
                    String portName = servicePort.getKey().substring(0, 1).toUpperCase() + servicePort.getKey().substring(1);
                    credentials.put(prefix + portName + "Port", String.format("%d", servicePort.getValue()));
                }
                endpoint.getCertSpec().ifPresent(certSpec -> {
                    String cert = getAuthApi().getCert(certSpec.getSecretName(), addressSpace.getNamespace());
                    credentials.put(prefix + "Cert.pem", cert);
                });
            }
            return Response.status(Response.Status.CREATED).entity(new BindResponse(credentials)).build();

        } catch (Exception e) {
            throw new InternalServerErrorException("Exception interacting with auth service", e);
        }



 // TODO: return 200 OK, when binding already exists

    }

    private void createGroupMapping(KeycloakClient keycloakClient, AddressSpace addressSpace, String userId, List<String> groupIds) throws IOException, GeneralSecurityException  {
        for(String groupId : groupIds) {

            keycloakClient.joinGroup(addressSpace.getName(), userId, groupId);
        }

    }

    private List<String> ensureAllGroups(KeycloakClient keycloakClient, AddressSpace addressSpace, List<String> allGroups) throws IOException, GeneralSecurityException {
        List<String> groupIds = new ArrayList<>(allGroups.size());
        Map<String, List<GroupRepresentation>> existingGroups = getAllExistingGroups(keycloakClient, addressSpace);
        for(String group : allGroups) {
            if(existingGroups.containsKey(group)) {
                groupIds.add(existingGroups.get(group).get(0).getId());
            } else {
                groupIds.add(keycloakClient.createGroup(addressSpace.getName(), group).getId());
            }
        }
        return groupIds;
    }

    private Map<String, List<GroupRepresentation>> getAllExistingGroups(KeycloakClient keycloakClient, AddressSpace addressSpace) {
        return keycloakClient.getGroups(addressSpace.getName()).stream()
                .collect(Collectors.groupingBy(GroupRepresentation::getName));
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
