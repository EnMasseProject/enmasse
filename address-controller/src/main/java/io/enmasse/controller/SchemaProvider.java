/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpacePlan;
import io.enmasse.address.model.Schema;

public interface SchemaProvider {
    Schema getSchema();
    void copyIntoNamespace(AddressSpacePlan plan, String namespace);
}
