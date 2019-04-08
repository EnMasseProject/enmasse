/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.admin.model.v1.DoneableKafkaInfraConfig;
import io.enmasse.admin.model.v1.KafkaInfraConfig;
import io.enmasse.admin.model.v1.KafkaInfraConfigList;
import io.enmasse.k8s.api.cache.*;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;

public class KubeKafkaInfraConfigApi implements KafkaInfraConfigApi, ListerWatcher<KafkaInfraConfig, KafkaInfraConfigList> {

    private final NamespacedOpenShiftClient client;
    private final String namespace;
    private final CustomResourceDefinition customResourceDefinition;

    public KubeKafkaInfraConfigApi(NamespacedOpenShiftClient client, String namespace, CustomResourceDefinition customResourceDefinition) {
        this.client = client;
        this.namespace = namespace;
        this.customResourceDefinition = customResourceDefinition;
    }

    @Override
    public KafkaInfraConfigList list(ListOptions listOptions) {
        return client.customResources(customResourceDefinition, KafkaInfraConfig.class, KafkaInfraConfigList.class, DoneableKafkaInfraConfig.class).inNamespace(namespace).list();
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watch(io.fabric8.kubernetes.client.Watcher<KafkaInfraConfig> watcher, ListOptions listOptions) {
        RequestConfig requestConfig = new RequestConfigBuilder()
                .withRequestTimeout(listOptions.getTimeoutSeconds())
                .build();
        return client.withRequestConfig(requestConfig).call(c ->
                c.customResources(customResourceDefinition, KafkaInfraConfig.class, KafkaInfraConfigList.class, DoneableKafkaInfraConfig.class).inNamespace(namespace).withResourceVersion(listOptions.getResourceVersion()).watch(watcher));
    }

    @Override
    public Watch watchKafkaInfraConfigs(Watcher<KafkaInfraConfig> watcher, Duration resyncInterval) {
        WorkQueue<KafkaInfraConfig> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        Reflector.Config<KafkaInfraConfig, KafkaInfraConfigList> config = new Reflector.Config<>();
        config.setClock(Clock.systemUTC());
        config.setExpectedType(KafkaInfraConfig.class);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(queue);
        config.setProcessor(map -> {
            if (queue.hasSynced()) {
                watcher.onUpdate(new ArrayList<>(queue.list()));
            }
        });

        Reflector<KafkaInfraConfig, KafkaInfraConfigList> reflector = new Reflector<>(config);
        Controller controller = new Controller(reflector);
        controller.start();
        return controller;
    }
}
