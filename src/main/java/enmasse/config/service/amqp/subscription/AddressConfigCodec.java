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

package enmasse.config.service.amqp.subscription;

import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.config.service.model.Config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Codec for addressing config
 */
public class AddressConfigCodec {

    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_STORE_AND_FORWARD = "store_and_forward";
    private static final String FIELD_MULTICAST = "multicast";

    public static void encodeJson(ObjectNode parent, Config config) {
        String key = config.getValue(FIELD_ADDRESS);
        ObjectNode node = parent.putObject(key);
        node.put(FIELD_STORE_AND_FORWARD, Boolean.parseBoolean(config.getValue(FIELD_STORE_AND_FORWARD)));
        node.put(FIELD_MULTICAST, Boolean.parseBoolean(config.getValue(FIELD_MULTICAST)));
    }

    public static Map<String, String> encodeLabels(String address, boolean storeAndForward, boolean multicast) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(FIELD_ADDRESS, address);
        data.put(FIELD_STORE_AND_FORWARD, String.valueOf(storeAndForward));
        data.put(FIELD_MULTICAST, String.valueOf(multicast));
        return data;
    }

    public static Config decodeLabels(Map<String, String> labelMap) {
        Map<String, String> labelCopy = new LinkedHashMap<>(labelMap);
        return key -> labelCopy.get(key);
    }

    public static Config encodeConfig(String address, boolean storeAndForward, boolean multicast) {
        return decodeLabels(encodeLabels(address, storeAndForward, multicast));
    }
}
