/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.v1.Either;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.k8s.api.AddressSpaceApi;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * HTTP API for operating on addresses within an address space
 */
@Path("/apis/enmasse.io/v1alpha1/namespaces/{namespace}/addresses")
public class HttpAddressService extends HttpAddressServiceBase {
    public HttpAddressService(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider) {
        super(addressSpaceApi, schemaProvider);
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
    public Response getAddressList(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @QueryParam("address") String address, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return super.getAddressList(securityContext, namespace, null, address, labelSelector);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response getAddress(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressName") String address) throws Exception {
        String addressSpace = parseAddressSpace(address);
        return super.getAddress(securityContext, namespace, addressSpace, address);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createAddress(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @PathParam("namespace") String namespace, @NotNull Address payload) throws Exception {
        String addressSpace = parseAddressSpace(payload.getName());
        return super.createAddress(securityContext, uriInfo, namespace, addressSpace, Either.<Address, AddressList>createLeft(payload));
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response replaceAddresses(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @NotNull Address address) throws Exception {
        String addressSpace = parseAddressSpace(address.getName());
        return super.replaceAddresses(securityContext, namespace, addressSpace, address);
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response deleteAddress(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressName") String addressName) throws Exception {
        String addressSpace = parseAddressSpace(addressName);
        return super.deleteAddress(securityContext, namespace, addressSpace, addressName);
    }
}
