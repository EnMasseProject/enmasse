/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpacePlan;
import io.enmasse.address.model.v1.SchemaProvider;

/**
 * Interface for Schema of the address model
 */
public interface SchemaApi extends SchemaProvider {
    /**
     * Copy address space plan and referenced address plans and resource definitions into namespace;
     */
    void copyIntoNamespace(AddressSpacePlan addressSpacePlan, String otherNamespace);
}
