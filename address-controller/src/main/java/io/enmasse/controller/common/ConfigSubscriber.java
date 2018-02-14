/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.common;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Watcher;

import java.io.IOException;

/**
 * Represents a component subscribes to config map updates.
 */
public interface ConfigSubscriber {
    void configUpdated(Watcher.Action action, ConfigMap configMap) throws IOException;
}
