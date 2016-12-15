package enmasse.config.service.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.config.service.openshift.MessageEncoder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encodes a set of address configs to an AMQP message
 */
public class ConfigMessageEncoder implements MessageEncoder {
    private static final Logger log = LoggerFactory.getLogger(ConfigMessageEncoder.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Message encode(Set<HasMetadata> resources) throws IOException {
        List<Config> configs = resources.stream()
                .map(h -> AddressConfigCodec.decodeLabels(h.getMetadata().getLabels()))
                .collect(Collectors.toList());
        Message message = Message.Factory.create();
        ObjectNode root = mapper.createObjectNode();
        for (Config config : configs) {
            AddressConfigCodec.encodeJson(root, config);
        }
        message.setBody(createBody(root));
        message.setContentType("application/json");
        log.info("Address config was updated to '" + ((AmqpValue)message.getBody()).getValue() + "'");
        return message;
    }

    private Section createBody(JsonNode root) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator generator = mapper.getFactory().createGenerator(baos);
        mapper.writeTree(generator, root);
        return new AmqpValue(baos.toString("UTF-8"));
    }
}
