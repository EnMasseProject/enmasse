package enmasse.config.service.config;

import enmasse.config.service.kubernetes.MessageEncoder;
import io.enmasse.address.model.impl.k8s.v1.address.AddressCodec;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encodes a set of address configs to an AMQP message
 */
public class ConfigMessageEncoder implements MessageEncoder<ConfigResource> {
    private static final Logger log = LoggerFactory.getLogger(ConfigMessageEncoder.class.getName());
    private final AddressCodec addressCodec = new AddressCodec();

    @Override
    public Message encode(Set<ConfigResource> resources) throws IOException {
        Message message = Message.Factory.create();
        List<byte[]> addressList = new ArrayList<>();
        for (ConfigResource config : resources) {
            Map<String, String> data = config.getData();

            // Don't decode data, but pack it into a list instead
            addressList.add(data.get("json").getBytes("UTF-8"));
        }
        message.setBody(createBody(addressList));
        message.setContentType("application/json");
        return message;
    }

    private Section createBody(List<byte[]> addressList) throws IOException {
        return new AmqpValue(addressCodec.encodeAddressList(addressList));
    }
}
