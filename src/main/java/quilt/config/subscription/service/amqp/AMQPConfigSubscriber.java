package quilt.config.subscription.service.amqp;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.proton.ProtonSender;
import quilt.config.subscription.service.model.ConfigSubscriber;
import org.apache.commons.compress.utils.Charsets;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates AMQP message on config updates, converting the mapping to a JSON object. Values that are valid JSON strings, will be
 * embedded in the new object.
 *
 *  TODO: This should probably be done once for the config, not for each subscriber.
 *
 * @author lulf
 */
public class AMQPConfigSubscriber implements ConfigSubscriber {
    private static final Logger log = Logger.getLogger(AMQPConfigSubscriber.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProtonSender sender;
    public AMQPConfigSubscriber(ProtonSender sender) {
        this.sender = sender;
    }

    @Override
    public void configUpdated(String name, String version, Map<String, String> values) {
        Message message = Message.Factory.create();

        try {
            JsonNode root = encodeConfigAsJson(values);
            message.setBody(createBody(root));
            message.setContentType("application/json");
            sender.send(message, delivery -> {
                log.log(Level.FINE, "Client has received update");
            });
            log.log(Level.FINE, "Notified client on update");
        } catch (Exception e) {
            log.log(Level.WARNING, "Error converting map to JSON: " + e.getMessage());
        }

    }

    private Section createBody(JsonNode root) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator generator = mapper.getFactory().createGenerator(baos);
        mapper.writeTree(generator, root);
        return new AmqpValue(baos.toString(Charsets.UTF_8.name()));
    }

    private JsonNode encodeConfigAsJson(Map<String, String> values) throws IOException {
        ObjectNode node = mapper.createObjectNode();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            encodeJson(node, entry.getKey(), entry.getValue());
        }
        return node;
    }

    private void encodeJson(ObjectNode node, String key, String value) throws IOException {
        try {
            node.set(key, mapper.readTree(value));
        } catch (IOException e) {
            log.log(Level.INFO, "Unable to decode, returning as string");
            node.put(key, value);
        }
    }
}
