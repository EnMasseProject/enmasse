package enmasse.address.controller.api.v3.http;

import enmasse.address.controller.api.v3.Address;
import enmasse.address.controller.api.v3.AddressList;
import enmasse.address.controller.api.v3.ApiHandler;
import enmasse.address.controller.model.TenantId;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v3/tenant/{tenant}/address")
public class MultitenantAddressingService extends AddressingServiceBase {
    public MultitenantAddressingService(@Context ApiHandler apiHandler) {
        super(apiHandler);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response listAddresses(@PathParam("tenant") String tenant) {
        return listAddresses(TenantId.fromString(tenant));
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddresses(@PathParam("tenant") String tenant, AddressList addressList) {
        return putAddresses(TenantId.fromString(tenant), addressList);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response appendAddress(@PathParam("tenant") String tenant, Address address) {
        return appendAddress(TenantId.fromString(tenant), address);
    }

    @GET
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddress(@PathParam("tenant") String tenant, @PathParam("address") String address) {
        return getAddress(TenantId.fromString(tenant), address);
    }

    @PUT
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddress(@PathParam("tenant") String tenant, @PathParam("address") String address) {
        return putAddress(TenantId.fromString(tenant), address);
    }

    @DELETE
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddress(@PathParam("tenant") String tenant, @PathParam("address") String address) {
        return deleteAddress(TenantId.fromString(tenant), address);
    }
}
