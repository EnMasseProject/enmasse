package enmasse.address.controller.api.v3.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.address.controller.api.v3.Address;
import enmasse.address.controller.api.v3.AddressList;
import enmasse.address.controller.api.v3.ApiHandler;
import enmasse.address.controller.api.v3.ApiResource;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Handles address updates based on an AMQP message. Responses are sent to an optional {@link ResponseHandler}
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

    private final ApiHandler apiHandler;

    public AddressingService(ApiHandler apiHandler) {
        this.apiHandler = apiHandler;
    }

    public void handleMessage(Message message, Optional<ResponseHandler> responseHandler) throws IOException {
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
            handleGet(message, responseHandler);
        } else if (METHOD_PUT.equals(method)) {
            handlePut(message, responseHandler);
        } else if (METHOD_POST.equals(method)) {
            handleAppend(message, responseHandler);
        } else if (METHOD_DELETE.equals(method)) {
            handleDelete(message, responseHandler);
        }
    }

    private void handleGet(Message message, Optional<ResponseHandler> responseHandler) throws IOException {
        Optional<String> address = getAddressProperty(message);

        if (address.isPresent()) {
            sendResponse(responseHandler, apiHandler.getAddress(address.get()));
        } else {
            sendResponse(responseHandler, apiHandler.getAddresses());
        }
    }

    private void handlePut(Message message, Optional<ResponseHandler> responseHandler) throws IOException {
        String json = (String)((AmqpValue) message.getBody()).getValue();
        String kind = getKind(json);
        if (Address.kind().equals(kind)) {
            Address address = mapper.readValue(json, Address.class);
            sendResponse(responseHandler, apiHandler.putAddress(address));
        } else if (AddressList.kind().equals(kind)) {
            AddressList list = mapper.readValue(json, AddressList.class);
            sendResponse(responseHandler, apiHandler.putAddresses(list));
        }
    }

    private void handleAppend(Message message, Optional<ResponseHandler> responseHandler) throws IOException {
        String json = (String)((AmqpValue) message.getBody()).getValue();
        String kind = getKind(json);
        if (Address.kind().equals(kind)) {
            Address address = mapper.readValue(json, Address.class);
            sendResponse(responseHandler, apiHandler.appendAddress(address));
        } else if (AddressList.kind().equals(kind)) {
            AddressList list = mapper.readValue(json, AddressList.class);
            sendResponse(responseHandler, apiHandler.appendAddresses(list));
        }
    }


    private void handleDelete(Message message, Optional<ResponseHandler> responseHandler) throws IOException {
        Optional<String> address = getAddressProperty(message);

        if (address.isPresent()) {
            AddressList list = apiHandler.deleteAddress(address.get());
            sendResponse(responseHandler, list);
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

    private static void sendResponse(Optional<ResponseHandler> responseHandler, Object object) throws JsonProcessingException {
        if (responseHandler.isPresent()) {
            responseHandler.get().sendResponse(createResponse(object));
        }
    }

    private static String getKind(String json) throws IOException {
        ApiResource resource = mapper.readValue(json, ApiResource.class);
        return resource.getKind();
    }
}
