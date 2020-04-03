/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.jdbc.config.JdbcDeviceProperties;
import io.enmasse.iot.jdbc.store.device.AbstractDeviceManagementStore;
import io.enmasse.iot.jdbc.store.device.DeviceStores;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.noop.NoopSpan;
import io.opentracing.noop.NoopTracerFactory;

public class JdbcDeviceRegsitryTenantCleaner extends AbstractJdbcCleaner {

    private final static Logger log = LoggerFactory.getLogger(JdbcDeviceRegsitryTenantCleaner.class);

    private final AbstractDeviceManagementStore devices;

    public JdbcDeviceRegsitryTenantCleaner() throws Exception {

        final ObjectMapper mapper = new ObjectMapper();
        final JdbcDeviceProperties devices = mapper.readValue(System.getenv("jdbc.devices"), JdbcDeviceProperties.class);

        if (devices.getManagement() != null) {
            this.devices = DeviceStores.store(
                    this.vertx,
                    NoopTracerFactory.create(),
                    devices,
                    JdbcDeviceProperties::getManagement,
                    DeviceStores.managementStoreFactory());
        } else {
            this.devices = null;
        }

    }

    public void run() throws Exception {

        if (this.devices == null) {
            log.info("Nothing to clean up. Management connection is not set.");
            // nothing to do
            return;
        }

        var f = this.devices.dropTenant(this.tenantId, NoopSpan.INSTANCE.context());

        MoreFutures.map(f)
                .whenComplete((r, e) -> logResult("Devices", r, e))
                .get();

    }

    @Override
    public void close() throws Exception {
        this.devices.close();
        super.close();
    }

}
