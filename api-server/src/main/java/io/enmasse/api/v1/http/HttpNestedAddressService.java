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
@Path("/apis/enmasse.io/v1alpha1/namespaces/{namespace}/addressspaces/{addressSpace}/addresses")
public class HttpNestedAddressService extends HttpAddressServiceBase {
    public HttpNestedAddressService(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider) {
        super(addressSpaceApi, schemaProvider);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAddressList(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @QueryParam("address") String address, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return super.getAddressList(securityContext, namespace, addressSpace, address, labelSelector);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response getAddress(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @PathParam("addressName") String address) throws Exception {
        return super.getAddress(securityContext, namespace, addressSpace, address);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createAddress(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @NotNull Either<Address, AddressList> payload) throws Exception {
        return super.createAddress(securityContext, uriInfo, namespace, addressSpace, payload);
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response replaceAddresses(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @NotNull Address address) throws Exception {
        return super.replaceAddresses(securityContext, namespace, addressSpace, address);
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response deleteAddress(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @PathParam("addressName") String addressName) throws Exception {
        return super.deleteAddress(securityContext, namespace, addressSpace, addressName);
    }
}
