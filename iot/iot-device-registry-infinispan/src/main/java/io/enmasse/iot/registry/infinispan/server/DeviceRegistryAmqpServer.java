/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.server;

import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.service.amqp.AmqpServiceBase;
import org.eclipse.hono.util.Constants;
import org.springframework.stereotype.Component;

@Component
public final class DeviceRegistryAmqpServer extends AmqpServiceBase<ServiceConfigProperties> {

    @Override
    protected String getServiceName() {
        return Constants.SERVICE_NAME_DEVICE_REGISTRY;
    }

}