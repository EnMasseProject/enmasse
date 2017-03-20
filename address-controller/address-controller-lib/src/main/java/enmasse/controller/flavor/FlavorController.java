/*
 * Copyright 2017 Red Hat Inc.
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
package enmasse.controller.flavor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.controller.common.ConfigAdapter;
import enmasse.controller.common.ConfigSubscriber;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * Watches OpenShift configmap of flavors and updates the flavor manager
 */
public class FlavorController extends AbstractVerticle implements ConfigSubscriber {
    private static final Logger log = LoggerFactory.getLogger(FlavorController.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private final FlavorManager flavorManager;
    private final ConfigAdapter configAdapter;

    public FlavorController(OpenShiftClient openShiftClient, FlavorManager flavorManager) {
        this.configAdapter = new ConfigAdapter(openShiftClient, "flavor", this);
        this.flavorManager = flavorManager;
    }

    @Override
    public void start() {
        configAdapter.start();
    }

    @Override
    public void stop() {
        configAdapter.stop();
    }

    @Override
    public void configUpdated(ConfigMap configMap) throws IOException {

        if (configMap.getData().containsKey("json")) {
            log.debug("Got new config for " + configMap.getMetadata().getName() + " with data: " + configMap.getData().get("json"));
            JsonNode root = mapper.readTree(configMap.getData().get("json"));
            flavorManager.flavorsUpdated(FlavorParser.parse(root));
        } else {
            log.debug("Got empty config for " + configMap.getMetadata().getName());
            flavorManager.flavorsUpdated(Collections.emptyMap());
        }
    }
}
