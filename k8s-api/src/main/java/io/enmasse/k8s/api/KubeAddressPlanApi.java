/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressPlanList;
import io.enmasse.admin.model.v1.DoneableAddressPlan;
import io.enmasse.k8s.api.cache.*;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;

public class KubeAddressPlanApi implements AddressPlanApi, ListerWatcher<AddressPlan, AddressPlanList> {

    private final NamespacedOpenShiftClient client;
    private final String namespace;
    private final CustomResourceDefinition addressSpacePlanDefinition;

    public KubeAddressPlanApi(NamespacedOpenShiftClient client, String namespace, CustomResourceDefinition addressSpacePlanDefinition) {
        this.client = client;
        this.namespace = namespace;
        this.addressSpacePlanDefinition = addressSpacePlanDefinition;
    }

    @Override
    public AddressPlanList list(ListOptions listOptions) {
        return client.customResources(addressSpacePlanDefinition, AddressPlan.class, AddressPlanList.class, DoneableAddressPlan.class).inNamespace(namespace).list();
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watch(io.fabric8.kubernetes.client.Watcher<AddressPlan> watcher, ListOptions listOptions) {
        RequestConfig requestConfig = new RequestConfigBuilder()
                .withRequestTimeout(listOptions.getTimeoutSeconds())
                .build();
        return client.withRequestConfig(requestConfig).call(c ->
                c.customResources(addressSpacePlanDefinition, AddressPlan.class, AddressPlanList.class, DoneableAddressPlan.class).inNamespace(namespace).withResourceVersion(listOptions.getResourceVersion()).watch(watcher));
    }

    @Override
    public Watch watchAddressPlans(Watcher<AddressPlan> watcher, Duration resyncInterval) {
        WorkQueue<AddressPlan> queue = new FifoQueue<>(config -> config.getMetadata().getName());
        Reflector.Config<AddressPlan, AddressPlanList> config = new Reflector.Config<>();
        config.setClock(Clock.systemUTC());
        config.setExpectedType(AddressPlan.class);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(queue);
        config.setProcessor(map -> {
            if (queue.hasSynced()) {
                watcher.onUpdate(new ArrayList<>(queue.list()));
            }
        });

        Reflector<AddressPlan, AddressPlanList> reflector = new Reflector<>(config);
        Controller controller = new Controller(reflector);
        controller.start();
        return controller;
    }
}
