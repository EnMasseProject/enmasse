/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.service.base;

import static io.enmasse.iot.model.v1.IoTCrd.tenant;
import static java.util.Optional.empty;

import java.util.Optional;

import io.enmasse.iot.model.v1.DoneableIoTTenant;
import io.enmasse.iot.model.v1.IoTTenant;
import io.enmasse.iot.model.v1.IoTTenantList;
import io.enmasse.model.CustomResourceDefinitions;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public final class IoTTenants {

    static {
        CustomResourceDefinitions.registerAll();
    }

    private IoTTenants() {}

    public static MixedOperation<IoTTenant, IoTTenantList, DoneableIoTTenant, Resource<IoTTenant, DoneableIoTTenant>> clientForTenant(final KubernetesClient client) {
        return client
                .customResources(tenant(),
                        IoTTenant.class, IoTTenantList.class, DoneableIoTTenant.class);
    }

    public static Optional<IoTTenant> getTenant(final KubernetesClient client, final String tenantName) {
        return getTenant(clientForTenant(client), tenantName);
   }

    public static Optional<IoTTenant> getTenant(final MixedOperation<IoTTenant, IoTTenantList, DoneableIoTTenant, Resource<IoTTenant, DoneableIoTTenant>> projects,
                                                final String tenantName) {

        final String[] toks = tenantName.split("\\.", 2);

        if (toks.length < 2) {
            return empty();
        }

        final String namespace = toks[0];
        final String name = toks[1];

        final IoTTenant tenant = projects
                .inNamespace(namespace)
                .withName(name)
                .get();

        return Optional.ofNullable(tenant);

    }

}
