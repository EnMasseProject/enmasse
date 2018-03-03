/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api.lastoperation;

import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.osb.api.OSBServiceBase;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressSpaceApi;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Collections;

@Path(OSBServiceBase.BASE_URI + "/service_instances/{instanceId}/last_operation")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBLastOperationService extends OSBServiceBase {

    public OSBLastOperationService(AddressSpaceApi addressSpaceApi, AuthApi authApi, SchemaProvider schemaProvider) {
        super(addressSpaceApi, authApi, schemaProvider);
    }

    @GET
    public Response getLastOperationStatus(@Context SecurityContext securityContext,
                                           @PathParam("instanceId") String instanceId,
                                           @QueryParam("service_id") String serviceId,
                                           @QueryParam("plan_id") String planId,
                                           @QueryParam("operation") String operation) throws Exception {

        log.info("Received last_operation request for instance {}, operation {}, service id {}, plan id {}",
                instanceId, operation, serviceId, planId);
        verifyAuthorized(securityContext, ResourceVerb.get);

        AddressSpace instance = findAddressSpaceByInstanceId(instanceId)
                .orElse(null);

        if(instance == null) {
            log.info("No such address space found");
            return Response.status(Response.Status.GONE).entity(Collections.EMPTY_MAP).build();
        } else {
            LastOperationResponse response;
            if (instance.getStatus().isReady()) {
                log.info("Address space is ready");
                response = new LastOperationResponse(LastOperationState.SUCCEEDED, "All required pods are ready.");
            } else {
                log.info("Address space is not yet ready");
                response = new LastOperationResponse(LastOperationState.IN_PROGRESS, "Waiting for pods to be ready");
            }
            // TODO LastOperationState.FAILED ?

            return Response.ok(response).build();
        }
    }

}
