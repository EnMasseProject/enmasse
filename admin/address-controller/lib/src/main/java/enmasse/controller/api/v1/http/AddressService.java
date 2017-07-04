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
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.AddressSpaceId;
import enmasse.controller.model.Instance;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.impl.k8s.v1.address.AddressCodec;
import io.enmasse.address.model.impl.k8s.v1.address.DecodeContext;
import io.enmasse.address.model.impl.types.standard.StandardAddressDecodeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;

/**
 * Represents an address service to get addresses.
 */
@Path("/v1/addresses/{addressSpace}")
public class AddressService {
    private static final Logger log = LoggerFactory.getLogger(AddressService.class.getName());
    private final AddressApiHelper apiHelper;
    private final InstanceApi instanceApi;
    private final AddressCodec addressCodec = new AddressCodec();

    // TODO: Parameterizing this could potentially make this class generic
    private final DecodeContext decodeContext = new StandardAddressDecodeContext();

    public AddressService(InstanceApi instanceApi) {
        this.apiHelper = new AddressApiHelper(instanceApi);
        this.instanceApi = instanceApi;
    }

    private AddressSpaceId getId(String addressSpaceId) {
        for (Instance instance : instanceApi.listInstances()) {
            if (addressSpaceId.equals(instance.id().getId())) {
                return instance.id();
            }
        }
        throw new NotFoundException("No address space with id " + addressSpaceId + " found");
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAddressList(@PathParam("addressSpace") String addressSpace) {
        try {

            return Response.ok(addressCodec.encodeAddressList(apiHelper.getAddresses(getId(addressSpace)))).build();
        } catch (Exception e) {
            log.error("Exception getting address list", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{address}")
    public Response getAddress(@PathParam("addressSpace") String addressSpace, @PathParam("address") String address) {
        try {
            Optional<Address> found = apiHelper.getAddress(getId(addressSpace), address);
            if (!found.isPresent()) {
                return Response.status(404).build();
            } else {
                return Response.ok(addressCodec.encodeAddress(found.get())).build();
            }
        } catch (Exception e) {
            log.error("Exception getting address", e);
            return Response.serverError().build();
        }
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response appendAddress(@PathParam("addressSpace") String addressSpace, byte[] body) {
        try {
            Object decoded = addressCodec.decode(decodeContext, body);
            if (decoded instanceof Address) {
                apiHelper.appendAddress(getId(addressSpace), (Address) decoded);
            } else if (decoded instanceof Set) {
                apiHelper.appendAddresses(getId(addressSpace), (Set<Address>) decoded);
            } else {
                // TODO: Return something more useful
                return Response.serverError().build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            log.error("Exception getting address", e);
            return Response.serverError().build();
        }
    }
}
