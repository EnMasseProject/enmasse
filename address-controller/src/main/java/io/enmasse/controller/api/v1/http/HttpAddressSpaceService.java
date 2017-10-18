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
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path(HttpAddressSpaceService.BASE_URI)
public class HttpAddressSpaceService {

    public static final String BASE_URI = "/v1/addressspaces";

    private static final Logger log = LoggerFactory.getLogger(HttpAddressSpaceService.class.getName());
    private final AddressSpaceApi addressSpaceApi;
    public HttpAddressSpaceService(AddressSpaceApi addressSpaceApi) {
        this.addressSpaceApi = addressSpaceApi;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddressSpaceList() {
        try {
            return Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces())).build();
        } catch (Exception e) {
            log.error("Exception getting address space list", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response getAddressSpace(@PathParam("addressSpace") String addressSpaceName) {
        try {
            return addressSpaceApi.getAddressSpaceWithName(addressSpaceName)
                    .map(addressSpace -> Response.ok(addressSpace).build())
                    .orElse(Response.status(404).build());
        } catch (Exception e) {
            log.error("Exception getting address space {}", addressSpaceName, e);
            return Response.serverError().build();
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createAddressSpace(AddressSpace addressSpace) {
        try {
            addressSpaceApi.createAddressSpace(addressSpace);
            return Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces())).build();
        } catch (Exception e) {
            log.error("Exception creating address space {}", addressSpace, e);
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("{addressSpace}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddressSpace(@PathParam("addressSpace") String addressSpaceName) {
        try {
            Optional<AddressSpace> addressSpace = addressSpaceApi.getAddressSpaceWithName(addressSpaceName);
            if (addressSpace.isPresent()) {
                addressSpaceApi.deleteAddressSpace(addressSpace.get());
                return Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces())).build();
            } else {
                return Response.status(404).build();
            }
        } catch (Exception e) {
            log.error("Exception deleting address space {}", addressSpaceName, e);
            return Response.serverError().build();
        }
    }
}
