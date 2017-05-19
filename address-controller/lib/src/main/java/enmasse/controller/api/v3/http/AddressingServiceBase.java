package enmasse.controller.api.v3.http;

import enmasse.controller.address.v3.Address;
import enmasse.controller.address.v3.AddressList;
import enmasse.controller.api.v3.AddressApi;
import enmasse.controller.model.InstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.Optional;

public class AddressingServiceBase {
    private static final Logger log = LoggerFactory.getLogger(AddressingServiceBase.class.getName());
    private final AddressApi addressApi;

    public AddressingServiceBase(AddressApi addressApi) {
        this.addressApi = addressApi;
    }

    public Response listAddresses(InstanceId instanceId) {
        try {
            return Response.ok(addressApi.getAddresses(instanceId)).build();
        } catch (Exception e) {
            log.warn("Error listing addresses", e);
            return Response.serverError().build();
        }
    }

    public Response putAddresses(InstanceId instanceId, AddressList addressList) {
        try {
            return Response.ok(addressApi.putAddresses(instanceId, addressList)).build();
        } catch (Exception e) {
            log.warn("Error putting addresses", e);
            return Response.serverError().build();
        }
    }

    public Response appendAddress(InstanceId instanceId, Address address) {
        try {
            return Response.ok(addressApi.appendAddress(instanceId, address)).build();
        } catch (Exception e) {
            log.warn("Error appending addresses", e);
            return Response.serverError().build();
        }
    }

    public Response getAddress(InstanceId instanceId, String address) {
        try {
            Optional<Address> addr = addressApi.getAddress(instanceId, address);

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

    public Response putAddress(InstanceId instanceId, String address) {
        return Response.status(405).build();
    }

    public Response deleteAddress(InstanceId instanceId, String address) {
        try {
            return Response.ok(addressApi.deleteAddress(instanceId, address)).build();
        } catch (Exception e) {
            log.warn("Error deleting address", e);
            return Response.serverError().build();
        }
    }
}
