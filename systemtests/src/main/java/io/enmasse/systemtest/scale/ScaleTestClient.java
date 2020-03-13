/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.scale.metrics.ScaleTestClientMetricsClient;

public class ScaleTestClient <T extends ScaleTestClientMetricsClient>{

    private final ScaleTestClientConfiguration configuration;
    private final T metricsClient;

    private int connections;

    public ScaleTestClient(ScaleTestClientConfiguration configuration, T metricsClient) {
        this.configuration = configuration;
        this.metricsClient = metricsClient;
    }

    public static  <T extends ScaleTestClientMetricsClient> ScaleTestClient<T> from(ScaleTestClientConfiguration configuration, T metricsClient) {
        return new ScaleTestClient<>(configuration, metricsClient);
    }

    public ScaleTestClientConfiguration getConfiguration() {
        return configuration;
    }

    public T getMetricsClient() {
        return metricsClient;
    }

    public int getConnections() {
        return connections;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    public AddressType getAddressesType() {
        return configuration.getAddressesType();
    }
}
