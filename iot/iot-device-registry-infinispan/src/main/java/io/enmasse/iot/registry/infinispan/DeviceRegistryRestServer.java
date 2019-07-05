/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.service.http.HttpServiceBase;
import org.springframework.stereotype.Component;

/**
 * Default REST server for Hono's example device registry.
 */
@Component
public class DeviceRegistryRestServer extends HttpServiceBase<ServiceConfigProperties> {

}
