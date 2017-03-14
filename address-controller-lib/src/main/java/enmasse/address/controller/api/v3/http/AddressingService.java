package enmasse.address.controller.api.v3.http;

import enmasse.address.controller.api.v3.Address;
import enmasse.address.controller.api.v3.AddressList;
import enmasse.address.controller.api.v3.ApiHandler;
import enmasse.address.controller.model.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("/v3/address")
public class AddressingService {
    private static final Logger log = LoggerFactory.getLogger(AddressingService.class.getName());
    private final ApiHandler apiHandler;
    private final TenantId tenantId = TenantId.fromString("mytenant");

    public AddressingService(@Context ApiHandler apiHandler) {
        this.apiHandler = apiHandler;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response listAddresses() {
        try {
            return Response.ok(apiHandler.getAddresses(tenantId)).build();
        } catch (Exception e) {
            log.warn("Error listing addresses", e);
            return Response.serverError().build();
        }
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddresses(AddressList addressList) {
        try {
            return Response.ok(apiHandler.putAddresses(tenantId, addressList)).build();
        } catch (Exception e) {
            log.warn("Error putting addresses", e);
            return Response.serverError().build();
        }
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response appendAddress(Address address) {
        try {
            return Response.ok(apiHandler.appendAddress(tenantId, address)).build();
        } catch (Exception e) {
            log.warn("Error appending addresses", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddress(@PathParam("address") String address) {
        try {
            Optional<Address> addr = apiHandler.getAddress(tenantId, address);

            if (addr.isPresent()) {
                return Response.ok(addr.get()).build();
            } else {
                return Response.status(404).build();
            }
        } catch (Exception e) {
            log.warn("Error getting address", e);
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddress(@PathParam("address") String address) {
        return Response.status(405).build();
    }

    @DELETE
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddress(@PathParam("address") String address) {
        try {
            return Response.ok(apiHandler.deleteAddress(tenantId, address)).build();
        } catch (Exception e) {
            log.warn("Error deleting address", e);
            return Response.serverError().build();
        }
    }
}
