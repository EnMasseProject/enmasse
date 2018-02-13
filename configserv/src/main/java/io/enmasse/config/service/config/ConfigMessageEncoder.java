/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
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
public class ConfigMessageEncoder implements MessageEncoder<Address> {
    private static final Logger log = LoggerFactory.getLogger(ConfigMessageEncoder.class.getName());
    private static final ObjectMapper mapper = CodecV1.getMapper();

    @Override
    public Message encode(Set<Address> resources) throws IOException {
        Message message = Message.Factory.create();
        // TODO: Avoid so much decode/encode
        AddressList addressList = new AddressList(resources);
        message.setSubject("enmasse.io/v1/AddressList");
        message.setBody(createBody(addressList));
        message.setContentType("application/json");
        return message;
    }

    private Section createBody(List<Address> addressList) throws IOException {
        return new AmqpValue(mapper.writeValueAsString(addressList));
    }
}
