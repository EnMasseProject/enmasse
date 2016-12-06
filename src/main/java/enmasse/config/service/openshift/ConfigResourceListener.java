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

package enmasse.config.service.openshift;

import enmasse.config.service.amqp.subscription.AddressConfigCodec;
import enmasse.config.service.model.Config;
import enmasse.config.service.model.ConfigSubscriber;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.ClientOperation;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Listeners for ConfigMaps and DeploymentConfigs and creates an aggregate configuration.
 */
public class ConfigResourceListener implements OpenshiftResourceListener {
    private final List<ConfigSubscriber> subscriberList = new ArrayList<>();

    private final List<Config> configList = new ArrayList<>();

    /**
     * Subscribe for updates to this configuration.
     *
     * @param subscriber The subscriber handle.
     */
    public synchronized void subscribe(ConfigSubscriber subscriber) {
        subscriberList.add(subscriber);
        // Notify only when we have values
        if (!configList.isEmpty()) {
            subscriber.configUpdated(configList);
        }
    }

    /**
     * Notify subscribers that the set of configs has been updated.
     */
    private void notifySubscribers() {
        List<Config> list = Collections.unmodifiableList(configList);
        subscriberList.stream().forEach(subscription -> subscription.configUpdated(list));
    }

    @Override
    public synchronized void resourcesUpdated(Set<HasMetadata> resources) {
        configList.clear();
        configList.addAll(resources.stream()
                .map(resource -> createFromResourceLabels(resource))
                .collect(Collectors.toList()));
        notifySubscribers();
    }

    @Override
    public ClientOperation<? extends HasMetadata, ?, ?, ?>[] getOperations(OpenShiftClient client) {
        ClientOperation<? extends HasMetadata, ?, ?, ?> [] ops = new ClientOperation[2];
        ops[0] = client.configMaps();
        ops[1] = client.deploymentConfigs();
        return ops;
    }

    private Config createFromResourceLabels(HasMetadata resource) {
        return AddressConfigCodec.decodeLabels(resource.getMetadata().getLabels());
    }
}
