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

package enmasse.controller.api.v3.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.model.Flavor;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class AmqpFlavorsApiTest {
    private static ObjectMapper mapper = new ObjectMapper();
    private FlavorManager flavorManager;
    private FlavorsService flavorsService;

    @Before
    public void setup() {
        flavorManager = new FlavorManager();
        flavorsService = new FlavorsService(flavorManager);
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put("flavor1", new Flavor.Builder("flavor1", "template1").type("queue").description("Simple queue").build());
        flavorMap.put("flavor2", new Flavor.Builder("flavor2", "template2").type("topic").description("Simple topic").build());
        flavorManager.flavorsUpdated(flavorMap);
    }

    @Test
    public void testList() throws IOException {
        Message response = doRequest("GET", "", Optional.empty());
        ObjectNode json = decodeJson(response);

        assertThat(json.get("kind").asText(), is("FlavorList"));
        assertThat(json.get("items").size(), is(2));
        assertThat(json.get("items").get(0).get("spec").get("type").asText(), is("queue"));
        assertThat(json.get("items").get(0).get("spec").get("description").asText(), is("Simple queue"));
        assertThat(json.get("items").get(1).get("spec").get("type").asText(), is("topic"));
        assertThat(json.get("items").get(1).get("spec").get("description").asText(), is("Simple topic"));
    }

    @Test
    public void testGet() throws IOException {
        Message response = doRequest("GET", "", Optional.of("flavor1"));
        ObjectNode data = decodeJson(response);

        assertThat(data.get("kind").asText(), is("Flavor"));
        assertThat(data.get("metadata").get("name").asText(), is("flavor1"));
        assertThat(data.get("spec").get("type").asText(), is("queue"));
        assertThat(data.get("spec").get("description").asText(), is("Simple queue"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnknown() throws IOException {
        doRequest("GET", "", Optional.of("unknown"));
    }

    private static ObjectNode decodeJson(Message message) throws IOException {
        return mapper.readValue((String)((AmqpValue)message.getBody()).getValue(), ObjectNode.class);
    }


    private Message doRequest(String method, Object body, Optional<String> addressProperty) throws IOException {
        Message message = Message.Factory.create();
        message.setAddress("$flavor");
        message.setContentType("application/json");
        message.setBody(new AmqpValue(mapper.writeValueAsString(body)));
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("method", method);
        addressProperty.ifPresent(address -> properties.put("flavor", address));
        message.setApplicationProperties(new ApplicationProperties(properties));

        return flavorsService.handleMessage(message);
    }
}
