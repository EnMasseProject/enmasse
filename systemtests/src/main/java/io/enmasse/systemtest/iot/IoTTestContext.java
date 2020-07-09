/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.DoneableIoTConfig;
import io.enmasse.iot.model.v1.DoneableIoTProject;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
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
    IoTConfig getConfig();

    /**
     * Get the IoTProject instance that was used during creation.
     *
     * If you need to retrieve the current state of the project, use the {@link #tenant()} method.
     *
     * @return The original instance.
     */
    IoTProject getProject();
    AmqpClient getConsumerClient();

    default Resource<IoTConfig, DoneableIoTConfig> config() {
        return Kubernetes.iotConfigs(getConfig().getMetadata().getNamespace()).withName(getConfig().getMetadata().getName());
    }

    default Resource<IoTProject, DoneableIoTProject> tenant() {
        return Kubernetes.iotTenants(getProject().getMetadata().getNamespace()).withName(getProject().getMetadata().getName());
    }

    /**
     * Get the Hono tenant ID.
     */
    default String getTenantId() {
        return IoTUtils.getTenantId(getProject());
    }

}
