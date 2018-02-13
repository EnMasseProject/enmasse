/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.bind;

import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.controller.api.osb.v2.EmptyResponse;
import io.enmasse.controller.api.osb.v2.OSBExceptions;
import io.enmasse.controller.api.osb.v2.OSBServiceBase;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.k8s.api.AddressSpaceApi;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path(OSBServiceBase.BASE_URI + "/service_instances/{instanceId}/service_bindings/{bindingId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBBindingService extends OSBServiceBase {

    public OSBBindingService(AddressSpaceApi addressSpaceApi, String namespace) {
        super(addressSpaceApi, namespace);
    }

    @PUT
    public Response bindServiceInstance(@Context SecurityContext securityContext, @PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId, BindRequest bindRequest) {
        log.info("Received bind request for instance {}, binding {} (service id {}, plan id {})",
                instanceId, bindingId, bindRequest.getServiceId(), bindRequest.getPlanId());
        verifyAuthorized(securityContext, ResourceVerb.get);
        AddressSpace addressSpace = findAddressSpaceByAddressUuid(instanceId)
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        Address address = findAddress(addressSpace, instanceId)  // TODO: replace this and findInstanceByAddressUuid so it returns both objects
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        if (bindRequest.getServiceId() == null) {
            throw OSBExceptions.badRequestException("Missing service_id in request");
        }
        if (bindRequest.getPlanId() == null) {
            throw OSBExceptions.badRequestException("Missing plan_id in request");
        }

        Map<String, String> credentials = new HashMap<>();
        credentials.put("namespace", addressSpace.getNamespace());
        for (Endpoint endpoint : addressSpace.getEndpoints()) {
            endpoint.getHost().ifPresent(host -> credentials.put(endpoint.getName(), host));
            credentials.put("internal-" + endpoint.getName() + "-host", endpoint.getService() + "." + addressSpace.getNamespace() + ".svc.cluster.local");
        }
        credentials.put("destination-address", address.getAddress());

        BindResponse response = new BindResponse(credentials);
        return Response.status(Response.Status.CREATED).entity(response).build();        // TODO: return 200 OK, when binding already exists
    }

    @DELETE
    public Response unbindServiceInstance(@Context SecurityContext securityContext, @PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId) {
        log.info("Received unbind request for instance {}, binding {}", instanceId, bindingId);
        verifyAuthorized(securityContext, ResourceVerb.get);
        AddressSpace addressSpace = findAddressSpaceByAddressUuid(instanceId)
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        return Response.ok(new EmptyResponse()).build();
    }

}
