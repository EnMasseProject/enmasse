/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

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
public class JdbcDeviceConnection {

    @JsonUnwrapped
    private CommonDeviceRegistry commonDeviceRegistry;

    @JsonUnwrapped
    private ServiceConfig serviceConfig;
    @JsonUnwrapped
    private CommonServiceConfig commonServiceConfig;

    private JdbcDeviceConnectionServer server;

    public CommonDeviceRegistry getCommonDeviceRegistry() {
        return commonDeviceRegistry;
    }

    public void setCommonDeviceRegistry(CommonDeviceRegistry commonDeviceRegistry) {
        this.commonDeviceRegistry = commonDeviceRegistry;
    }

    public JdbcDeviceConnectionServer getServer() {
        return server;
    }

    public void setServer(JdbcDeviceConnectionServer server) {
        this.server = server;
    }

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public CommonServiceConfig getCommonServiceConfig() {
        return commonServiceConfig;
    }

    public void setCommonServiceConfig(CommonServiceConfig commonServiceConfig) {
        this.commonServiceConfig = commonServiceConfig;
    }

}
