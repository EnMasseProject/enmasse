/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.AddressSpaceResolver;
import io.enmasse.address.model.v1.SchemaProvider;
import io.enmasse.controller.api.RbacSecurityContext;
import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.controller.api.osb.v2.OSBExceptions;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.concurrent.Callable;
import java.util.function.Function;

@Path(HttpAddressSpaceService.BASE_URI)
public class HttpAddressSpaceService {

    static final String BASE_URI = "/apis/enmasse.io/v1/addressspaces";

    private static final Logger log = LoggerFactory.getLogger(HttpAddressSpaceService.class.getName());
    private final String namespace;
    private final SchemaProvider schemaProvider;

    private final AddressSpaceApi addressSpaceApi;
    public HttpAddressSpaceService(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider, String namespace) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
        this.namespace = namespace;
    }

    private Response doRequest(SecurityContext securityContext, ResourceVerb verb, String errorMessage, Callable<Response> request) throws Exception {
        try {
            verifyAuthorized(securityContext, verb);
            return request.call();
        } catch (Exception e) {
            log.error(errorMessage, e);
            throw e;
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
    public Response getAddressSpaceList(@Context SecurityContext securityContext) throws Exception {
        return doRequest(securityContext, ResourceVerb.list, "Error getting address space list", () ->
                Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces())).build());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response getAddressSpace(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName) throws Exception {
        return doRequest(securityContext, ResourceVerb.get, "Error getting address space " + addressSpaceName, () ->
            addressSpaceApi.getAddressSpaceWithName(addressSpaceName)
                    .map(addressSpace -> Response.ok(addressSpace).build())
                    .orElseThrow(() -> new NotFoundException("Address space " + addressSpaceName + " not found")));
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createAddressSpace(@Context SecurityContext securityContext, @NotNull  AddressSpace input) throws Exception {
        return doRequest(securityContext, ResourceVerb.create, "Error creating address space " + input.getName(), () -> {

            AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
            addressSpaceResolver.validate(input);
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
    public Response deleteAddressSpace(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName) throws Exception {
        return doRequest(securityContext, ResourceVerb.delete, "Error deleting address space " + addressSpaceName, () -> {
            AddressSpace addressSpace = addressSpaceApi.getAddressSpaceWithName(addressSpaceName)
                    .orElseThrow(() -> new NotFoundException("Unable to find address space " + addressSpaceName));
            addressSpaceApi.deleteAddressSpace(addressSpace);
            return Response.ok(addressSpace).build();
        });
    }
}
