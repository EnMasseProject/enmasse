/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.amqp.Artemis;
import io.enmasse.amqp.SyncRequestClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;

class BrokerStatusCollector {
    private final Kubernetes kubernetes;
    private final BrokerClientFactory brokerClientFactory;

    BrokerStatusCollector(Kubernetes kubernetes, BrokerClientFactory brokerClientFactory) {
        this.kubernetes = kubernetes;
        this.brokerClientFactory = brokerClientFactory;
    }

    long getQueueMessageCount(String queue, String clusterId) throws Exception {
        long totalMessageCount = 0;
        for (Pod broker : kubernetes.listBrokers(clusterId)) {
            if (Readiness.isPodReady(broker)) {
                try (
                        SyncRequestClient brokerClient = brokerClientFactory.connectBrokerManagementClient(broker.getStatus().getPodIP(), 5673);
                        Artemis artemis = new Artemis(brokerClient);
                        ) {
                    totalMessageCount += artemis.getQueueMessageCount(queue);
                }
            }
        }
        return totalMessageCount;
    }
}
