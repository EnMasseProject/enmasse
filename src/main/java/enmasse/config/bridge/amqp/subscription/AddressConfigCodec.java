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

import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.config.bridge.model.ConfigMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Codec for addressing config
 */
public class AddressConfigCodec {

    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_STORE_AND_FORWARD = "store-and-forward";
    private static final String FIELD_STORE_AND_FORWARD_JSON = "store_and_forward";
    private static final String FIELD_MULTICAST = "multicast";

    public static void encode(ObjectNode parent, ConfigMap config) {
        String key = config.getData().get(FIELD_ADDRESS);
        ObjectNode node = parent.putObject(key);
        node.put(FIELD_STORE_AND_FORWARD_JSON, Boolean.parseBoolean(config.getData().get(FIELD_STORE_AND_FORWARD)));
        node.put(FIELD_MULTICAST, Boolean.parseBoolean(config.getData().get(FIELD_MULTICAST)));
    }

    public static ConfigMap encode(String address, boolean storeAndForward, boolean multicast) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(FIELD_ADDRESS, address);
        data.put(FIELD_STORE_AND_FORWARD, String.valueOf(storeAndForward));
        data.put(FIELD_MULTICAST, String.valueOf(multicast));
        return new ConfigMap(data);
    }
}
