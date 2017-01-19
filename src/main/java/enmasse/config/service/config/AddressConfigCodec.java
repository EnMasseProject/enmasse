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

package enmasse.config.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Codec for addressing config
 */
public class AddressConfigCodec {

    private static final String FIELD_ADDRESS = "addressList";
    private static final String FIELD_STORE_AND_FORWARD = "store_and_forward";
    private static final String FIELD_MULTICAST = "multicast";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void encodeJson(ObjectNode parent, Config config) {
        Set<String> addresses = parseAddressAnnotation(config.getValue(FIELD_ADDRESS));
        for (String address : addresses) {
            ObjectNode node = parent.putObject(address);
            node.put(FIELD_STORE_AND_FORWARD, Boolean.parseBoolean(config.getValue(FIELD_STORE_AND_FORWARD)));
            node.put(FIELD_MULTICAST, Boolean.parseBoolean(config.getValue(FIELD_MULTICAST)));
        }
    }

    public static Map<String, String> encodeLabels(boolean storeAndForward, boolean multicast) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(FIELD_STORE_AND_FORWARD, String.valueOf(storeAndForward));
        data.put(FIELD_MULTICAST, String.valueOf(multicast));
        return data;
    }

    public static Map<String, String> encodeAnnotations(String address) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(FIELD_ADDRESS, address);
        return data;
    }

    public static Config decodeConfig(ConfigResource resource) {
        Map<String, String> labelMap = resource.getLabels();
        Map<String, String> annotationMap = resource.getAnnotations();

        return key -> {
            if (labelMap.containsKey(key)) {
                return labelMap.get(key);
            } else {
                return annotationMap.get(key);
            }
        };
    }

    private static Set<String> parseAddressAnnotation(String jsonAddresses) {
        try {
            ArrayNode array = (ArrayNode) mapper.readTree(jsonAddresses);
            Set<String> lst = new HashSet<>();
            for (int i = 0; i < array.size(); i++) {
                lst.add(array.get(i).asText());
            }
            return lst;
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse address annotation '" + jsonAddresses + "'", e);
        }
    }
}
