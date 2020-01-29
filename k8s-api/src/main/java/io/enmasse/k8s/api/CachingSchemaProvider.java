/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.Schema;
import io.enmasse.address.model.SchemaBuilder;

public class CachingSchemaProvider implements SchemaProvider, Watcher<Schema> {
    private static final Logger log = LoggerFactory.getLogger(CachingSchemaProvider.class);
    private volatile Schema schema = null;
    private final List<SchemaListener> listeners = new CopyOnWriteArrayList<>();

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
        schema = new SchemaBuilder(items.get(0)).build();
        log.info("Schema updated: {}", schema.printSchema());
        for (SchemaListener listener : listeners) {
            listener.onUpdate(schema);
        }
    }
}
