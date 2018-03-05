/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.Schema;
import io.enmasse.k8s.api.Watcher;

import java.util.Set;

public class CachingSchemaProvider implements SchemaProvider, Watcher<Schema> {
    private volatile Schema schema;

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void resourcesUpdated(Set<Schema> resources) {
        if (resources.isEmpty()) {
            schema = null;
        } else {
            schema = resources.iterator().next();
        }
    }
}
