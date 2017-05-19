package enmasse.controller.api.v3.http;

import enmasse.controller.address.v3.Address;
import enmasse.controller.address.v3.AddressList;
import enmasse.controller.api.v3.AddressApi;
import enmasse.controller.model.InstanceId;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v3/instance/{instance}/address")
public class MultiInstanceAddressingService extends AddressingServiceBase {
    public MultiInstanceAddressingService(@Context AddressApi addressApi) {
        super(addressApi);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response listAddresses(@PathParam("instance") String instance) {
        return listAddresses(InstanceId.withId(instance));
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddresses(@PathParam("instance") String instance, AddressList addressList) {
        return putAddresses(InstanceId.withId(instance), addressList);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response appendAddress(@PathParam("instance") String instance, Address address) {
        return appendAddress(InstanceId.withId(instance), address);
    }

    @GET
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddress(@PathParam("instance") String instance, @PathParam("address") String address) {
        return getAddress(InstanceId.withId(instance), address);
    }

    @PUT
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddress(@PathParam("instance") String instance, @PathParam("address") String address) {
        return putAddress(InstanceId.withId(instance), address);
    }

    @DELETE
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddress(@PathParam("instance") String instance, @PathParam("address") String address) {
        return deleteAddress(InstanceId.withId(instance), address);
    }
}
