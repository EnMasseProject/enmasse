package enmasse.controller.api.v3.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.controller.address.v3.Address;
import enmasse.controller.address.v3.AddressList;
import enmasse.controller.api.v3.AddressApi;
import enmasse.controller.api.v3.ApiResource;
import enmasse.controller.model.InstanceId;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Handles address updates based on an AMQP message.
 */
public class AddressingService {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String APPLICATION_JSON = "application/json";
    private static final String PROPERTY_METHOD = "method";
    private static final String PROPERTY_ADDRESS = "address";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_DELETE = "DELETE";

    private final InstanceId instance;
    private final AddressApi addressApi;

    public AddressingService(InstanceId instance, AddressApi addressApi) {
        this.instance = instance;
        this.addressApi = addressApi;
    }

    public Message handleMessage(Message message) throws Exception {
        ApplicationProperties properties = message.getApplicationProperties();
        if (properties == null) {
            throw new IllegalArgumentException("Missing message properties");
        }
        Map propertyMap = properties.getValue();

        if (!propertyMap.containsKey(PROPERTY_METHOD)) {
            throw new IllegalArgumentException("Property 'method' is missing");
        }
        String method = (String) propertyMap.get(PROPERTY_METHOD);

        if (METHOD_GET.equals(method)) {
            return handleGet(message);
        } else if (METHOD_PUT.equals(method)) {
            return handlePut(message);
        } else if (METHOD_POST.equals(method)) {
            return handleAppend(message);
        } else if (METHOD_DELETE.equals(method)) {
            return handleDelete(message);
        } else {
            throw new IllegalArgumentException("Unknown method " + method);
        }
    }

    private Message handleGet(Message message) throws IOException {
        Optional<String> address = getAddressProperty(message);

        if (address.isPresent()) {
            Optional<Address> addr = addressApi.getAddress(instance, address.get());
            if (addr.isPresent()) {
                return createResponse(addr.get());
            } else {
                throw new IllegalArgumentException("Address " + address.get() + " not found");
            }
        } else {
            return createResponse(addressApi.getAddresses(instance));
        }
    }

    private Message handlePut(Message message) throws Exception {
        String json = (String)((AmqpValue) message.getBody()).getValue();
        String kind = getKind(json);
        if (Address.kind().equals(kind)) {
            Address address = mapper.readValue(json, Address.class);
            return createResponse(addressApi.putAddress(instance, address));
        } else if (AddressList.kind().equals(kind)) {
            AddressList list = mapper.readValue(json, AddressList.class);
            return createResponse(addressApi.putAddresses(instance, list));
        } else {
            throw new IllegalArgumentException("Unknown kind " + kind);
        }
    }

    private Message handleAppend(Message message) throws Exception {
        String json = (String)((AmqpValue) message.getBody()).getValue();
        String kind = getKind(json);
        if (Address.kind().equals(kind)) {
            Address address = mapper.readValue(json, Address.class);
            return createResponse(addressApi.appendAddress(instance, address));
        } else if (AddressList.kind().equals(kind)) {
            AddressList list = mapper.readValue(json, AddressList.class);
            return createResponse(addressApi.appendAddresses(instance, list));
        } else {
            throw new IllegalArgumentException("Unknown kind " + kind);
        }
    }


    private Message handleDelete(Message message) throws IOException {
        Optional<String> address = getAddressProperty(message);

        if (address.isPresent()) {
            AddressList list = addressApi.deleteAddress(instance, address.get());
            return createResponse(list);
        } else {
            throw new IllegalArgumentException("Address to delete was not specified in application properties");
        }
    }

    private static Optional<String> getAddressProperty(Message message) {
        return Optional.ofNullable(message.getApplicationProperties())
                .map(ApplicationProperties::getValue)
                .map(propertyMap -> (String)propertyMap.get(PROPERTY_ADDRESS));
    }

    private static Message createResponse(Object object) throws JsonProcessingException {
        Message response = Message.Factory.create();
        response.setContentType(APPLICATION_JSON);
        response.setBody(new AmqpValue(mapper.writeValueAsString(object)));
        return response;
    }

    private static String getKind(String json) throws IOException {
        ApiResource resource = mapper.readValue(json, ApiResource.class);
        return resource.getKind();
    }
}
