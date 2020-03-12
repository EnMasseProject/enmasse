/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.scale.metrics.ScaleTestClientMetricsClient;

public class ScaleTestClient <X extends ScaleTestClientMetricsClient>{

    private final ScaleTestClientConfiguration configuration;
    private final X metricsClient;

    public ScaleTestClient(ScaleTestClientConfiguration configuration, X metricsClient) {
        this.configuration = configuration;
        this.metricsClient = metricsClient;
    }

    public static  <X extends ScaleTestClientMetricsClient> ScaleTestClient<X> from(ScaleTestClientConfiguration configuration, X metricsClient) {
        return new ScaleTestClient<X>(configuration, metricsClient);
    }

    public ScaleTestClientConfiguration getConfiguration() {
        return configuration;
    }

    public X getMetricsClient() {
        return metricsClient;
    }

    //help methods

    public AddressType getAddressesType() {
        return configuration.getAddressesType();
    }
}
