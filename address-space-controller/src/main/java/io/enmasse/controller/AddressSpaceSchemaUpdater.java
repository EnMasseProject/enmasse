/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpaceSchema;
import io.enmasse.address.model.AddressSpaceSchemaList;
import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddressSpaceSchema;
import io.enmasse.address.model.Schema;
import io.enmasse.k8s.api.SchemaListener;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressSpaceSchemaUpdater implements SchemaListener {
    private static final Logger log = LoggerFactory.getLogger(AddressSpaceSchemaUpdater.class);

    private final MixedOperation<AddressSpaceSchema, AddressSpaceSchemaList, DoneableAddressSpaceSchema, Resource<AddressSpaceSchema, DoneableAddressSpaceSchema>> client;
    public AddressSpaceSchemaUpdater(KubernetesClient kubernetesClient) {
        this.client = kubernetesClient.customResources(CoreCrd.addresseSpaceSchemas(), AddressSpaceSchema.class, AddressSpaceSchemaList.class, DoneableAddressSpaceSchema.class);
    }

    @Override
    public void onUpdate(Schema newSchema) {
        for (AddressSpaceType type : newSchema.getAddressSpaceTypes()) {
            try {
                AddressSpaceSchema existing = client.withName(type.getName()).get();
                AddressSpaceSchema updated = AddressSpaceSchema.fromAddressSpaceType(type, newSchema.getAuthenticationServices());
                if (existing == null) {
                    client.create(updated);
                } else if (!updated.getSpec().equals(existing.getSpec())) {
                    client.createOrReplace(updated);
                }
            } catch (Exception e) {
                if (e instanceof KubernetesClientException) {
                    log.warn("Error updating schema for address space type '" + type.getName() + "'. Status: '" + ((KubernetesClientException) e).getStatus() + "'. Code: " + ((KubernetesClientException) e).getCode(), e);
                } else {
                    log.warn("Error updating schema for address space type '" + type.getName() + "'", e);
                }
            }
        }
    }
}
