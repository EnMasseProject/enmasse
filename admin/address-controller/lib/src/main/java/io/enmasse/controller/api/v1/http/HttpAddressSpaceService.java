package io.enmasse.controller.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.k8s.api.AddressSpaceApi;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/addressspaces")
public class HttpAddressSpaceService {
    private final AddressSpaceApi addressSpaceApi;
    public HttpAddressSpaceService(AddressSpaceApi addressSpaceApi) {
        this.addressSpaceApi = addressSpaceApi;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddressSpaceList() {
        return Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces())).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response getAddressSpace(@PathParam("addressSpace") String addressSpaceName) {
        return Response.ok(addressSpaceApi.getAddressSpaceWithName(addressSpaceName)
                .orElseThrow(() -> new NotFoundException("Address space " + addressSpaceName + " not found"))).build();
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createAddressSpaces(AddressSpace addressSpace) {
        addressSpaceApi.createAddressSpace(addressSpace);
        return Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces())).build();
    }

    @DELETE
    @Path("{addressSpace}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddressSpace(@PathParam("addressSpace") String addressSpaceName) {
        AddressSpace addressSpace = addressSpaceApi.getAddressSpaceWithName(addressSpaceName)
                .orElseThrow(() -> new NotFoundException("Address space " + addressSpaceName + " not found"));

        addressSpaceApi.deleteAddressSpace(addressSpace);
        return Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces())).build();
    }
}
