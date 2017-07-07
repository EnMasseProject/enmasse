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
package enmasse.controller.api.v1.http;

import enmasse.controller.api.v1.AddressApiHelper;
import enmasse.controller.k8s.api.AddressSpaceApi;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import org.jboss.resteasy.spi.NotImplementedYetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * HTTP API for operating on addresses within an address space
 */
@Path("/v1/addresses/{addressSpace}")
public class HttpAddressService {
    private static final Logger log = LoggerFactory.getLogger(HttpAddressService.class.getName());
    private final AddressApiHelper apiHelper;

    public HttpAddressService(AddressSpaceApi addressSpaceApi) {
        this.apiHelper = new AddressApiHelper(addressSpaceApi);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAddressList(@PathParam("addressSpace") String addressSpace) {
        try {

            return Response.ok(apiHelper.getAddresses(addressSpace)).build();
        } catch (Exception e) {
            log.error("Exception getting address list", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{address}")
    public Response getAddress(@PathParam("addressSpace") String addressSpaceName, @PathParam("address") String address) {
        try {
            Optional<Address> found = apiHelper.getAddress(addressSpaceName, address);
            if (!found.isPresent()) {
                return Response.status(404).build();
            } else {
                return Response.ok(found.get()).build();
            }
        } catch (Exception e) {
            log.error("Exception getting address", e);
            return Response.serverError().build();
        }
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
    public Response appendAddresses(@PathParam("addressSpace") String addressSpaceName, AddressList addressList) {
        try {
            AddressList addresses = apiHelper.appendAddresses(addressSpaceName, addressList);
            return Response.ok(addresses).build();
        } catch (Exception e) {
            log.error("Exception getting address", e);
            return Response.serverError().build();
        }
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response replaceAddresses(@PathParam("addressSpace") String addressSpaceName, AddressList addressList) {
        try {
            AddressList addresses = apiHelper.putAddresses(addressSpaceName, addressList);
            return Response.ok(addresses).build();
        } catch (Exception e) {
            log.error("Exception getting address", e);
            return Response.serverError().build();
        }
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
    @Path("{address}")
    public Response deleteAddress(@PathParam("addressSpace") String addressSpaceName, @PathParam("address") String address) {
        try {
            AddressList addresses = apiHelper.deleteAddress(addressSpaceName, address);
            return Response.ok(addresses).build();
        } catch (Exception e) {
            log.error("Exception getting address", e);
            return Response.serverError().build();
        }
    }
}
