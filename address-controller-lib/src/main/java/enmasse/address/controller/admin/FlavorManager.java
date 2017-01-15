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

package enmasse.address.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import enmasse.address.controller.model.Flavor;
import enmasse.address.controller.parser.FlavorParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the set of flavors supported by the address controller. Essentially a glorified map with timeouts for gets.
 */
public class FlavorManager implements FlavorRepository {
    private static final Logger log = LoggerFactory.getLogger(FlavorManager.class.getName());
    private volatile Map<String, Flavor> flavorMap = Collections.emptyMap();

    @Override
    public Flavor getFlavor(String flavorName, long timeoutInMillis) {
        long endTime = System.currentTimeMillis() + timeoutInMillis;
        Flavor flavor = null;
        try {
            do {
                flavor = flavorMap.get(flavorName);
                if (flavor == null) {
                    Thread.sleep(1000);
                }
            } while (System.currentTimeMillis() < endTime && flavor == null);
        } catch (InterruptedException e) {
            log.warn("Interrupted while retrieving flavor");
        }
        if (flavor == null) {
            String flavors = flavorMap.keySet().stream().collect(Collectors.joining(","));
            throw new IllegalArgumentException(String.format("No flavor with name '%s' exists, have [%s]", flavorName, flavors));
        }
        return flavor;
    }

    public void flavorsUpdated(Map<String, Flavor> flavorMap) {
        this.flavorMap = flavorMap;
        if (log.isInfoEnabled()) {
            String flavors = flavorMap.keySet().stream().collect(Collectors.joining(","));
            log.info(String.format("Got new set of flavors: [%s]", flavors));
        }

    }

    public void configUpdated(JsonNode jsonConfig) {
        flavorsUpdated(FlavorParser.parse(jsonConfig));
    }
}
