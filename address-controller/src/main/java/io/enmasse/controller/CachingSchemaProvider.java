/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpacePlan;
import io.enmasse.address.model.Schema;
import io.enmasse.k8s.api.SchemaApi;
import io.enmasse.k8s.api.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class CachingSchemaProvider implements SchemaProvider, Watcher<Schema> {
    private static final Logger log = LoggerFactory.getLogger(CachingSchemaProvider.class);
    private final SchemaApi schemaApi;
    private volatile Schema schema = null;

    public CachingSchemaProvider(SchemaApi schemaApi) {
        this.schemaApi = schemaApi;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void onUpdate(Set<Schema> items) throws Exception {
        if (items.isEmpty()) {
            return;
        }
        log.info("Schema updated");
        schema = items.iterator().next();
    }

    @Override
    public void copyIntoNamespace(AddressSpacePlan plan, String namespace) {
        schemaApi.copyIntoNamespace(plan, namespace);
    }
}
