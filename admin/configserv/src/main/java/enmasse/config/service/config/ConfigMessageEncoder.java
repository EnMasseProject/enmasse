package enmasse.config.service.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.config.AddressConfigKeys;
import enmasse.config.service.kubernetes.MessageEncoder;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

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
            Map<String, String> data = config.getData();

            ObjectNode address = root.putObject(data.get(AddressConfigKeys.ADDRESS));
            address.put("store_and_forward", Boolean.parseBoolean(data.get(AddressConfigKeys.STORE_AND_FORWARD)));
            address.put("multicast", Boolean.parseBoolean(data.get(AddressConfigKeys.MULTICAST)));
            address.put("group_id", data.get(AddressConfigKeys.GROUP_ID));
        }
        message.setBody(createBody(root));
        message.setContentType("application/json");
        log.info("Address config encoded: '" + ((AmqpValue)message.getBody()).getValue() + "'");
        return message;
    }

    private static Section createBody(JsonNode root) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator generator = mapper.getFactory().createGenerator(baos);
        mapper.writeTree(generator, root);
        return new AmqpValue(baos.toString("UTF-8"));
    }
}
