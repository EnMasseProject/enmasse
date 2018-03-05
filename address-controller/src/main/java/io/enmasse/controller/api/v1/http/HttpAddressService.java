/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.v1.http;

import io.enmasse.address.model.v1.Either;
import io.enmasse.controller.SchemaProvider;
import io.enmasse.controller.api.osb.v2.OSBExceptions;
import io.enmasse.controller.api.v1.AddressApiHelper;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
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

/**
 * HTTP API for operating on addresses within an address space
 */
@Path("/apis/enmasse.io/v1/addresses/{addressSpace}")
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

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAddressList(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpace, @QueryParam("address") String address) throws Exception {
        return doRequest("Error listing addresses",() -> {
            AddressList list = apiHelper.getAddresses(securityContext, addressSpace);
            if (address == null) {
                return Response.ok(list).build();
            } else {
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
    @Path("{address}")
    public Response getAddress(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName, @PathParam("address") String address) throws Exception {
        return doRequest("Error getting address",
                () -> Response.ok(apiHelper.getAddress(securityContext, addressSpaceName, address)
                        .orElseThrow(() -> OSBExceptions.notFoundException("Address " + address + " not found")))
                        .build());
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response appendAddress(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName, @NotNull Either<Address, AddressList> body) throws Exception {
        checkNotNull(body);
        if (body.isLeft()) {
            AddressList list = new AddressList();
            list.add(body.getLeft());
            return appendAddresses(securityContext, addressSpaceName, list);
        } else {
            return appendAddresses(securityContext, addressSpaceName, body.getRight());
        }
    }

    private Response appendAddresses(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName, @NotNull  AddressList list) throws Exception {
        checkNotNull(list);
        return doRequest("Error appending addresses", () -> {
            AddressList addressList = setAddressSpace(addressSpaceName, list);
            verifyAddressSpace(addressSpaceName, addressList);
            addressList = apiHelper.appendAddresses(securityContext, addressSpaceName, addressList);
            return Response.ok(addressList).build();
        });
    }

    private void checkNotNull(Object object) {
        if (object == null) {
            throw new BadRequestException("Missing request body");
        }
    }

    private void verifyAddressSpace(String addressSpaceName, AddressList addressList) {
        for (Address address : addressList) {
            if (!address.getAddressSpace().equals(addressSpaceName)) {
                throw new IllegalArgumentException("Address space of " + address + " does not match address space in url: " + addressSpaceName);
            }
        }
    }

    private AddressList setAddressSpace(String addressSpaceName, AddressList addressList) {
        AddressList list = new AddressList();
        for (Address address : addressList) {
            if (address.getAddressSpace() == null) {
                Address.Builder ab = new Address.Builder(address);
                ab.setAddressSpace(addressSpaceName);
                list.add(ab.build());
            } else {
                list.add(address);
            }
        }
        return list;
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response replaceAddresses(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName, @NotNull  AddressList list) throws Exception {
        checkNotNull(list);
        return doRequest("Error updating addresses", () -> {
            AddressList addressList = setAddressSpace(addressSpaceName, list);
            verifyAddressSpace(addressSpaceName, addressList);
            AddressList addresses = apiHelper.putAddresses(securityContext, addressSpaceName, addressList);
            return Response.ok(addresses).build();
        });
    }

    //@PUT
    //@Produces({MediaType.APPLICATION_JSON})
    //@Consumes({MediaType.APPLICATION_JSON})
    //@Path("{address}")
    //public Response putAddress(@PathParam("addressSpace") String addressSpaceName, @PathParam("address") String address, Address body) {
    //    throw new NotImplementedYetException();
    //}

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteAllAddresses(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName) throws Exception {
        return doRequest("Error deleting addresses", () -> {
            AddressList addresses = apiHelper.putAddresses(securityContext, addressSpaceName, new AddressList());
            return Response.ok(addresses).build();
        });
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{address}")
    public Response deleteAddress(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName, @PathParam("address") String address) throws Exception {
        return doRequest("Error deleting address", () -> {
            AddressList addresses = apiHelper.deleteAddress(securityContext, addressSpaceName, address);
            return Response.ok(addresses).build();
        });
    }
}
