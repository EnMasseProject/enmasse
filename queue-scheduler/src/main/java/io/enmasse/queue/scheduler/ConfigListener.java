/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.queue.scheduler;

import io.enmasse.address.model.Address;

import java.util.Map;
import java.util.Set;

/**
 * Interface for someone interested in addressing config.
 */
public interface ConfigListener {
    void addressesChanged(Map<String, Set<Address>> addressMap);
}
