/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.config.bridge.amqp.subscription;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.config.bridge.model.Config;
import enmasse.config.bridge.model.ConfigSubscriber;
import io.vertx.proton.ProtonSender;
import org.apache.commons.compress.utils.Charsets;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Creates AMQP message on address config updates, converting the multiple config maps to a single JSON object
 * with the addressing config.
 */
public class AddressConfigSubscriber implements ConfigSubscriber {
    private static final Logger log = LoggerFactory.getLogger(AddressConfigSubscriber.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ProtonSender sender;

    public AddressConfigSubscriber(ProtonSender sender) {
        this.sender = sender;
    }

    @Override
    public void configUpdated(List<Config> values) {
        Message message = Message.Factory.create();

        try {
            ObjectNode root = mapper.createObjectNode();
            for (Config config : values) {
                AddressConfigCodec.encodeJson(root, config);
            }
            message.setBody(createBody(root));
            message.setContentType("application/json");
            log.info("Address config was updated to '" + ((AmqpValue)message.getBody()).getValue() + "'");
            sender.send(message);
        } catch (Exception e) {
            log.warn("Error converting map to JSON: " + e.getMessage());
        }

    }

    private Section createBody(JsonNode root) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator generator = mapper.getFactory().createGenerator(baos);
        mapper.writeTree(generator, root);
        return new AmqpValue(baos.toString(Charsets.UTF_8.name()));
    }
}
