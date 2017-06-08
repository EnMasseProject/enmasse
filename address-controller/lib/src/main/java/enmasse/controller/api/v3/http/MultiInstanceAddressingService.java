package enmasse.controller.api.v3.http;

import enmasse.controller.address.v3.Address;
import enmasse.controller.address.v3.AddressList;
import enmasse.controller.api.v3.AddressApiHelper;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v3/instance/{instance}/address")
public class MultiInstanceAddressingService extends AddressingServiceBase {

    private final InstanceApi instanceApi;

    public MultiInstanceAddressingService(@Context AddressApiHelper addressApi, InstanceApi instanceApi) {
        super(addressApi);
        this.instanceApi = instanceApi;
    }

    private InstanceId getId(String instanceId) {
        for (Instance instance : instanceApi.listInstances()) {
            if (instanceId.equals(instance.id().getId())) {
                return instance.id();
            }
        }
        throw new NotFoundException("No instance with id " + instanceId + " found");
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response listAddresses(@PathParam("instance") String instance) {
        return listAddresses(getId(instance));
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddresses(@PathParam("instance") String instance, AddressList addressList) {
        return putAddresses(getId(instance), addressList);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response appendAddress(@PathParam("instance") String instance, Address address) {
        return appendAddress(getId(instance), address);
    }

    @GET
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddress(@PathParam("instance") String instance, @PathParam("address") String address) {
        return getAddress(getId(instance), address);
    }

    @PUT
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddress(@PathParam("instance") String instance, @PathParam("address") String address) {
        return putAddress(getId(instance), address);
    }

    @DELETE
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddress(@PathParam("instance") String instance, @PathParam("address") String address) {
        return deleteAddress(getId(instance), address);
    }
}
