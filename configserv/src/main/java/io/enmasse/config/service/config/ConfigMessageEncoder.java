package io.enmasse.config.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.config.service.kubernetes.MessageEncoder;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.v1.CodecV1;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encodes a set of address configs to an AMQP message
 */
public class ConfigMessageEncoder implements MessageEncoder<ConfigResource> {
    private static final Logger log = LoggerFactory.getLogger(ConfigMessageEncoder.class.getName());
    private static final ObjectMapper mapper = CodecV1.getMapper();

    @Override
    public Message encode(Set<ConfigResource> resources) throws IOException {
        Message message = Message.Factory.create();
        // TODO: Avoid so much decode/encode
        AddressList addressList = new AddressList();
        for (ConfigResource config : resources) {
            Map<String, String> data = config.getData();
            addressList.add(mapper.readValue(data.get("config.json"), Address.class));
        }
        message.setSubject("enmasse.io/v1/AddressList");
        message.setBody(createBody(addressList));
        message.setContentType("application/json");
        return message;
    }

    private Section createBody(List<Address> addressList) throws IOException {
        return new AmqpValue(mapper.writeValueAsString(addressList));
    }
}
