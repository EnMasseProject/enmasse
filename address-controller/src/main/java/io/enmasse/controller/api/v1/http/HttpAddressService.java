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

import io.enmasse.controller.api.osb.v2.OSBExceptions;
import io.enmasse.controller.api.v1.AddressApiHelper;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public HttpAddressService(AddressSpaceApi addressSpaceApi) {
        this.apiHelper = new AddressApiHelper(addressSpaceApi);
    }

    private Response doRequest(String errorMessage, Callable<Response> request) {
        try {
            return request.call();
        } catch (ClientErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error(errorMessage, e);
            return Response.serverError().build();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAddressList(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpace) {
        return doRequest("Error listing addresses",
                () -> Response.ok(apiHelper.getAddresses(securityContext, addressSpace)).build());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{address}")
    public Response getAddress(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName, @PathParam("address") String address) {
        return doRequest("Error getting address",
                () -> Response.ok(apiHelper.getAddress(securityContext, addressSpaceName, address)
                        .orElseThrow(() -> OSBExceptions.notFoundException("Address " + address + " not found")))
                        .build());
    }

    //@POST
    //@Produces({MediaType.APPLICATION_JSON})
    //@Consumes({MediaType.APPLICATION_JSON})
    //public Response appendAddress(@PathParam("addressSpace") String addressSpaceName, Address address) {
    //    try {
    //        AddressList addresses = apiHelper.appendAddress(addressSpaceName, address);
    //        return Response.ok(addresses).build();
    //    } catch (Exception e) {
    //        log.error("Exception getting address", e);
    //        return Response.serverError().build();
    //    }
    //}


    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response appendAddresses(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName, AddressList list) {
        return doRequest("Error appending addresses", () -> {
            AddressList addressList = setAddressSpace(addressSpaceName, list);
            verifyAddressSpace(addressSpaceName, addressList);
            addressList = apiHelper.appendAddresses(securityContext, addressSpaceName, addressList);
            return Response.ok(addressList).build();
        });
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
    public Response replaceAddresses(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName, AddressList list) {
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
    public Response deleteAllAddresses(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName) {
        return doRequest("Error deleting addresses", () -> {
            AddressList addresses = apiHelper.putAddresses(securityContext, addressSpaceName, new AddressList());
            return Response.ok(addresses).build();
        });
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{address}")
    public Response deleteAddress(@Context SecurityContext securityContext, @PathParam("addressSpace") String addressSpaceName, @PathParam("address") String address) {
        return doRequest("Error deleting address", () -> {
            AddressList addresses = apiHelper.deleteAddress(securityContext, addressSpaceName, address);
            return Response.ok(addresses).build();
        });
    }
}
