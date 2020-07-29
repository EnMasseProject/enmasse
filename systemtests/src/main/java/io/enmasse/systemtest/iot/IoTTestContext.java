/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.DoneableIoTInfrastructure;
import io.enmasse.iot.model.v1.DoneableIoTTenant;
import io.enmasse.iot.model.v1.IoTInfrastructure;
import io.enmasse.iot.model.v1.IoTTenant;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.client.dsl.Resource;

public interface IoTTestContext extends AutoCloseable, DeviceFactory {

    /**
     * Get the IoTConfig instance that was used during creation.
     *
     * If you need to retrieve the current state of the configuration, use the {@link #config()} method.
     *
     * @return The original instance.
     */
    IoTInfrastructure getInfra();

    /**
     * Get the IoTTenant instance that was used during creation.
     *
     * If you need to retrieve the current state of the tenant, use the {@link #tenant()} method.
     *
     * @return The original instance.
     */
    IoTTenant getTenant();
    AmqpClient getConsumerClient();

    default Resource<IoTInfrastructure, DoneableIoTInfrastructure> config() {
        return Kubernetes.iotInfrastructures(getInfra().getMetadata().getNamespace()).withName(getInfra().getMetadata().getName());
    }

    default Resource<IoTTenant, DoneableIoTTenant> tenant() {
        return Kubernetes.iotTenants(getTenant().getMetadata().getNamespace()).withName(getTenant().getMetadata().getName());
    }

    /**
     * Get the Hono tenant ID.
     */
    default String getTenantId() {
        return IoTUtils.getTenantId(getTenant());
    }

}
