package enmasse.controller.api.v3.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.api.v3.*;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Handles address updates based on an AMQP message.
 */
public class FlavorsService {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String APPLICATION_JSON = "application/json";
    private static final String PROPERTY_METHOD = "method";
    private static final String PROPERTY_FLAVOR = "flavor";
    private static final String METHOD_GET = "GET";

    private final FlavorRepository repository;

    public FlavorsService(FlavorRepository repository) {
        this.repository = repository;
    }

    public Message handleMessage(Message message) throws IOException {
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
        } else {
            throw new IllegalArgumentException("Unknown method " + method);
        }
    }

    private Message handleGet(Message message) throws IOException {
        Optional<String> flavor = getFlavorProperty(message);

        if (flavor.isPresent()) {
            Optional<Flavor> flav = repository.getFlavor(flavor.get()).map(Flavor::new);
            if (flav.isPresent()) {
                return createResponse(flav.get());
            } else {
                throw new IllegalArgumentException("Flavor " + flavor.get() + " not found");
            }
        } else {
            return createResponse(FlavorList.fromSet(repository.getFlavors()));
        }
    }

    private static Optional<String> getFlavorProperty(Message message) {
        return Optional.ofNullable(message.getApplicationProperties())
                .map(ApplicationProperties::getValue)
                .map(propertyMap -> (String)propertyMap.get(PROPERTY_FLAVOR));
    }

    private static Message createResponse(Object object) throws JsonProcessingException {
        Message response = Message.Factory.create();
        response.setContentType(APPLICATION_JSON);
        response.setBody(new AmqpValue(mapper.writeValueAsString(object)));
        return response;
    }
}
