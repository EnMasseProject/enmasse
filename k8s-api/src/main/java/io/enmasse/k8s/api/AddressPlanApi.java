/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.admin.model.v1.AddressPlan;

import java.time.Duration;

public interface AddressPlanApi {
    Watch watchAddressPlans(Watcher<AddressPlan> watcher, Duration resyncInterval);
}
