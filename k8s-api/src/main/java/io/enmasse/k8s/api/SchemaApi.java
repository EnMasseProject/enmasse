/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Schema;

import java.time.Duration;

/**
 * Interface for Schema of the address model
 */
public interface SchemaApi {
    /**
     * Watch changes to schema
     */
    Watch watchSchema(Watcher<Schema> watcher, Duration resyncInterval) throws Exception;
}
