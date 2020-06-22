/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.api.model.Doneable;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"))
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class DeviceRegistryServiceConfig {
    private InfinispanDeviceRegistry infinispan;
    private JdbcDeviceRegistry jdbc;
    private ManagementConfig management;

    public InfinispanDeviceRegistry getInfinispan() {
        return infinispan;
    }

    public void setInfinispan(InfinispanDeviceRegistry infinispan) {
        this.infinispan = infinispan;
    }

    public void setJdbc(JdbcDeviceRegistry jdbc) {
        this.jdbc = jdbc;
    }

    public JdbcDeviceRegistry getJdbc() {
        return jdbc;
    }

    public void setManagement(ManagementConfig management) {
        this.management = management;
    }

    public ManagementConfig getManagement() {
        return management;
    }

}
