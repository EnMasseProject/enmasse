/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CachingSchemaProvider implements SchemaProvider, Watcher<Schema> {
    private static final Logger log = LoggerFactory.getLogger(CachingSchemaProvider.class);
    private volatile Schema schema = null;
    private final List<SchemaListener> listeners = new ArrayList<>();

    @Override
    public Schema getSchema() {
        return schema;
    }

    public void registerListener(SchemaListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onUpdate(List<Schema> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        schema = items.get(0);
        log.info("Schema updated: {}", schema.printSchema());
        for (SchemaListener listener : listeners) {
            listener.onUpdate(schema);
        }
    }
}
