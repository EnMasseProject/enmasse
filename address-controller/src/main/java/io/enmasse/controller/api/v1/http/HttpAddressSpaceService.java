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
package io.enmasse.controller.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.controller.api.RbacSecurityContext;
import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.controller.api.osb.v2.OSBExceptions;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.concurrent.Callable;

@Path(HttpAddressSpaceService.BASE_URI)
public class HttpAddressSpaceService {

    static final String BASE_URI = "/apis/enmasse.io/v1/addressspaces";

    private static final Logger log = LoggerFactory.getLogger(HttpAddressSpaceService.class.getName());
    private final String namespace;

    private final AddressSpaceApi addressSpaceApi;
    public HttpAddressSpaceService(AddressSpaceApi addressSpaceApi, String namespace) {
        this.addressSpaceApi = addressSpaceApi;
        this.namespace = namespace;
    }

    private Response doRequest(SecurityContext securityContext, ResourceVerb verb, String errorMessage, Callable<Response> request) {
        try {
            verifyAuthorized(securityContext, verb);
            return request.call();
        } catch (ClientErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error(errorMessage, e);
            return Response.serverError().build();
        }
    }

    private void verifyAuthorized(SecurityContext securityContext, ResourceVerb verb) {
        String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : null;
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, user))) {
            throw OSBExceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddressSpaceList(@Context SecurityContext securityContext) {
        return doRequest(securityContext, ResourceVerb.list, "Error getting address space list", () ->
                Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces())).build());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response getAddressSpace(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName) {
        return doRequest(securityContext, ResourceVerb.get, "Error getting address space " + addressSpaceName, () ->
            addressSpaceApi.getAddressSpaceWithName(addressSpaceName)
                    .map(addressSpace -> Response.ok(addressSpace).build())
                    .orElseThrow(() -> new NotFoundException("Address space " + addressSpaceName + " not found")));
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createAddressSpace(@Context SecurityContext securityContext, AddressSpace input) {
        return doRequest(securityContext, ResourceVerb.create, "Error creating address space " + input.getName(), () -> {

            AddressSpace addressSpace = input;
            if (securityContext.getUserPrincipal() != null) {
                addressSpace = new AddressSpace.Builder(addressSpace)
                        .setCreatedBy(securityContext.getUserPrincipal().getName())
                        .build();
            }
            addressSpaceApi.createAddressSpace(addressSpace);
            return Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces())).build();
        });
    }

    @DELETE
    @Path("{addressSpace}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddressSpace(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName) {
        return doRequest(securityContext, ResourceVerb.delete, "Error deleting address space " + addressSpaceName, () -> {
            AddressSpace addressSpace = addressSpaceApi.getAddressSpaceWithName(addressSpaceName)
                    .orElseThrow(() -> new NotFoundException("Unable to find address space " + addressSpaceName));
            addressSpaceApi.deleteAddressSpace(addressSpace);
            return Response.ok(addressSpace).build();
        });
    }
}
