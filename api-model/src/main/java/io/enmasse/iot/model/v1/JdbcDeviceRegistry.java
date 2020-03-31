/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import io.fabric8.kubernetes.api.model.Doneable;
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
@JsonInclude(NON_NULL)
public class JdbcDeviceRegistry {

    @JsonUnwrapped
    @JsonInclude(NON_DEFAULT)
    private CommonDeviceRegistry commonDeviceRegistry = new CommonDeviceRegistry();

    private JdbcRegistryServer server;

    public void setCommonDeviceRegistry(CommonDeviceRegistry commonDeviceRegistry) {
        this.commonDeviceRegistry = commonDeviceRegistry;
    }

    public CommonDeviceRegistry getCommonDeviceRegistry() {
        return commonDeviceRegistry;
    }

    public JdbcRegistryServer getServer() {
        return server;
    }

    public void setServer(JdbcRegistryServer server) {
        this.server = server;
    }

}
