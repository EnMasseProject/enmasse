/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.service.amqp.AmqpServiceBase;
import org.springframework.stereotype.Component;

@Component
public final class TenantAmqpService extends AmqpServiceBase<ServiceConfigProperties> {

    @Override
    protected String getServiceName() {
        return "EnMasse-IoT-TenantService";
    }
}
