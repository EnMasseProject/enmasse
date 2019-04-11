/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import io.enmasse.address.model.Address;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.api.common.UnprocessableEntityException;
import io.enmasse.k8s.api.AddressSpaceApi;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.Clock;

/**
 * HTTP API for operating on addresses within an address space
 */
@Path("/apis/enmasse.io/{versoin:v1alpha1|v1beta1}/namespaces/{namespace}/addresses")
public class HttpAddressService extends HttpAddressServiceBase {
    public HttpAddressService(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider, Clock clock) {
        super(addressSpaceApi, schemaProvider, clock);
    }

    private static String parseAddressSpace(String addressName) {
        String [] parts = addressName.split("\\.");
        if (parts.length < 2) {
            throw new BadRequestException("Address name '" + addressName + "' does not contain valid separator (.) to identify address space");
        }
        return parts[0];
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAddressList(@Context SecurityContext securityContext, @HeaderParam("Accept") String acceptHeader, @PathParam("namespace") String namespace, @QueryParam("address") String address, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return internalGetAddressList(securityContext, acceptHeader, namespace, null, address, labelSelector);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response getAddress(@Context SecurityContext securityContext, @HeaderParam("Accept") String acceptHeader, @PathParam("namespace") String namespace, @PathParam("addressName") String addressName) throws Exception {
        String addressSpace = parseAddressSpace(addressName);
        return internalGetAddress(securityContext, acceptHeader, namespace, addressSpace, addressName);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createAddress(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @PathParam("namespace") String namespace, @NotNull @Valid Address payload) throws Exception {
        if (payload.getMetadata().getName() == null) {
            throw new UnprocessableEntityException("Required value: name is required");
        }
        String addressSpace = parseAddressSpace(payload.getMetadata().getName());
        return internalCreateAddress(securityContext, uriInfo, namespace, addressSpace, payload);
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response replaceAddress(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressName") String addressName, @NotNull @Valid Address payload) throws Exception {
        checkAddressObjectNameNotNull(payload, addressName);
        String addressSpace = parseAddressSpace(payload.getMetadata().getName());
        return internalReplaceAddress(securityContext, namespace, addressSpace, addressName, payload);
    }

    @PATCH
    @Consumes({MediaType.APPLICATION_JSON_PATCH_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response patchAddress(@Context SecurityContext securityContext,
                                 @PathParam("namespace") String namespace,
                                 @PathParam("addressName") String addressName,
                                 @NotNull JsonPatch patch) throws Exception {
        String addressSpace = parseAddressSpace(addressName);
        return patchInternal(securityContext, namespace, addressSpace, addressName, patch::apply);
    }

    @PATCH
    @Consumes({"application/merge-patch+json", "application/strategic-merge-patch+json"})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response patchAddress(@Context SecurityContext securityContext,
                                 @PathParam("namespace") String namespace,
                                 @PathParam("addressName") String addressName,
                                 @NotNull JsonMergePatch patch) throws Exception {

        String addressSpace = parseAddressSpace(addressName);
        return patchInternal(securityContext, namespace, addressSpace, addressName, patch::apply);
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response deleteAddress(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressName") String addressName) throws Exception {
        String addressSpace = parseAddressSpace(addressName);
        return internalDeleteAddress(securityContext, namespace, addressSpace, addressName);
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddresses(@Context SecurityContext securityContext, @PathParam("namespace") String namespace) throws Exception {
        return internalDeleteAddresses(securityContext, namespace);
    }
}
