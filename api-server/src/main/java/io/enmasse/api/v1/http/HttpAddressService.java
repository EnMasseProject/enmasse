/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.v1.Either;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * HTTP API for operating on addresses within an address space
 */
@Path("/apis/enmasse.io/v1/namespaces/{namespace}/addressspaces/{addressSpace}/addresses")
public class HttpAddressService {
    private static final Logger log = LoggerFactory.getLogger(HttpAddressService.class.getName());
    private final AddressApiHelper apiHelper;

    public HttpAddressService(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider) {
        this.apiHelper = new AddressApiHelper(addressSpaceApi, schemaProvider);
    }

    private Response doRequest(String errorMessage, Callable<Response> request) throws Exception {
        try {
            return request.call();
        } catch (Exception e) {
            log.error(errorMessage, e);
            throw e;
        }
    }

    private static void verifyAuthorized(SecurityContext securityContext, String namespace, ResourceVerb verb) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "addresses"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAddressList(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @QueryParam("address") String address, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return doRequest("Error listing addresses",() -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);
            if (address == null) {
                if (labelSelector != null) {
                    Map<String, String> labels = AddressApiHelper.parseLabelSelector(labelSelector);
                    AddressList list = apiHelper.getAddressesWithLabels(namespace, addressSpace, labels);
                    return Response.ok(list).build();
                } else {
                    AddressList list = apiHelper.getAddresses(namespace, addressSpace);
                    return Response.ok(list).build();
                }
            } else {
                AddressList list = apiHelper.getAddresses(namespace, addressSpace);
                for (Address entity : list) {
                    if (entity.getAddress().equals(address)) {
                        return Response.ok(entity).build();
                    }
                }
                throw new NotFoundException("Address " + address + " not found");
            }
        });
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response getAddress(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @PathParam("addressName") String address) throws Exception {
        return doRequest("Error getting address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);
            return Response.ok(apiHelper.getAddress(namespace, addressSpace, address)
                    .orElseThrow(() -> Exceptions.notFoundException("Address " + address + " not found")))
                    .build();
        });
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createAddress(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @NotNull Either<Address, AddressList> payload) throws Exception {
        if (payload.isLeft()) {
            return createAddress(securityContext, uriInfo, namespace, addressSpace, payload.getLeft());
        } else {
            for (Address address : payload.getRight()) {
                createAddress(securityContext, uriInfo, namespace, addressSpace, address);
            }
            return Response.created(uriInfo.getAbsolutePathBuilder().build()).build();
        }
    }

    private Response createAddress(SecurityContext securityContext, UriInfo uriInfo, String namespace, String addressSpace, Address address) throws Exception {
        checkNotNull(address);
        Address finalAddress = setAddressDefaults(namespace, addressSpace, address);
        return doRequest("Error creating address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.create);
            Address created = apiHelper.createAddress(addressSpace, finalAddress);
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(created.getName());
            return Response.created(builder.build()).entity(created).build();
        });
    }

    private Address setAddressDefaults(String namespace, String addressSpace, Address address) {
        if (address.getNamespace() == null || address.getAddressSpace() != null) {
            Address.Builder builder = new Address.Builder(address);
            if (address.getNamespace() == null) {
                builder.setNamespace(namespace);
            }

            if (address.getAddressSpace() == null) {
                builder.setAddressSpace(addressSpace);
            }
            address = builder.build();
        }
        return address;
    }

    private void checkNotNull(Object object) {
        if (object == null) {
            throw new BadRequestException("Missing request body");
        }
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response replaceAddresses(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @NotNull Address address) throws Exception {
        checkNotNull(address);
        Address finalAddress = setAddressDefaults(namespace, addressSpace, address);
        return doRequest("Error updating address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.update);
            Address replaced = apiHelper.replaceAddress(addressSpace, finalAddress);
            return Response.ok(replaced).build();
        });
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{addressName}")
    public Response deleteAddress(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpace, @PathParam("addressName") String addressName) throws Exception {
        return doRequest("Error deleting address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            apiHelper.deleteAddress(namespace, addressSpace, addressName);
            return Response.ok().build();
        });
    }
}
