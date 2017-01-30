package enmasse.config.service.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.config.service.openshift.MessageEncoder;
import enmasse.config.AddressEncoder;
import enmasse.config.AddressDecoder;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encodes a set of address configs to an AMQP message
 */
public class ConfigMessageEncoder implements MessageEncoder<ConfigResource> {
    private static final Logger log = LoggerFactory.getLogger(ConfigMessageEncoder.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Message encode(Set<ConfigResource> resources) throws IOException {
        Message message = Message.Factory.create();
        ObjectNode root = mapper.createObjectNode();
        for (ConfigResource config : resources) {
            for (Map.Entry<String, String> entry : config.getData().entrySet()) {
                AddressDecoder decoder = new AddressDecoder(entry.getValue());
                AddressEncoder encoder = new AddressEncoder(root.putObject(entry.getKey()));
                // Don't encode flavor in address config
                encoder.encode(decoder.storeAndForward(), decoder.multicast(), Optional.empty());
            }
        }
        message.setBody(createBody(root));
        message.setContentType("application/json");
        log.info("Address config was updated to '" + ((AmqpValue)message.getBody()).getValue() + "'");
        return message;
    }

    private static Section createBody(JsonNode root) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator generator = mapper.getFactory().createGenerator(baos);
        mapper.writeTree(generator, root);
        return new AmqpValue(baos.toString("UTF-8"));
    }
}
