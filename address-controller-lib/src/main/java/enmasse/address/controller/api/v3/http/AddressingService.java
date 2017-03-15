package enmasse.address.controller.api.v3.http;

import enmasse.address.controller.api.v3.Address;
import enmasse.address.controller.api.v3.AddressList;
import enmasse.address.controller.api.v3.ApiHandler;
import enmasse.address.controller.model.InstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("/v3/address")
public class AddressingService extends AddressingServiceBase {
    private final InstanceId instanceId = InstanceId.fromString("myinstance");

    public AddressingService(@Context ApiHandler apiHandler) {
        super(apiHandler);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response listAddresses() {
        return listAddresses(instanceId);
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddresses(AddressList addressList) {
        return putAddresses(instanceId, addressList);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response appendAddress(Address address) {
        return appendAddress(instanceId, address);
    }

    @GET
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddress(@PathParam("address") String address) {
        return getAddress(instanceId, address);
    }

    @PUT
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddress(@PathParam("address") String address) {
        return putAddress(instanceId, address);
    }

    @DELETE
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddress(@PathParam("address") String address) {
        return deleteAddress(instanceId, address);
    }
}
