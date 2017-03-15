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

public class AddressingServiceBase {
    private static final Logger log = LoggerFactory.getLogger(AddressingServiceBase.class.getName());
    private final ApiHandler apiHandler;

    public AddressingServiceBase(ApiHandler apiHandler) {
        this.apiHandler = apiHandler;
    }

    public Response listAddresses(TenantId tenantId) {
        try {
            return Response.ok(apiHandler.getAddresses(tenantId)).build();
        } catch (Exception e) {
            log.warn("Error listing addresses", e);
            return Response.serverError().build();
        }
    }

    public Response putAddresses(TenantId tenantId, AddressList addressList) {
        try {
            return Response.ok(apiHandler.putAddresses(tenantId, addressList)).build();
        } catch (Exception e) {
            log.warn("Error putting addresses", e);
            return Response.serverError().build();
        }
    }

    public Response appendAddress(TenantId tenantId, Address address) {
        try {
            return Response.ok(apiHandler.appendAddress(tenantId, address)).build();
        } catch (Exception e) {
            log.warn("Error appending addresses", e);
            return Response.serverError().build();
        }
    }

    public Response getAddress(TenantId tenantId, String address) {
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

    public Response putAddress(TenantId tenantId, String address) {
        return Response.status(405).build();
    }

    public Response deleteAddress(TenantId tenantId, String address) {
        try {
            return Response.ok(apiHandler.deleteAddress(tenantId, address)).build();
        } catch (Exception e) {
            log.warn("Error deleting address", e);
            return Response.serverError().build();
        }
    }
}
