/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;

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
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServicesConfig {
    private DeviceRegistryServiceConfig deviceRegistry;
    private AuthenticationServiceConfig authentication;
    private TenantServiceConfig tenant;
    private CollectorConfig collector;
    private OperatorConfig operator;

    public DeviceRegistryServiceConfig getDeviceRegistry() {
        return deviceRegistry;
    }
    public void setDeviceRegistry(DeviceRegistryServiceConfig deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    public AuthenticationServiceConfig getAuthentication() {
        return authentication;
    }
    public void setAuthentication(AuthenticationServiceConfig authentication) {
        this.authentication = authentication;
    }

    public TenantServiceConfig getTenant() {
        return tenant;
    }
    public void setTenant(TenantServiceConfig tenant) {
        this.tenant = tenant;
    }

    public CollectorConfig getCollector() {
        return collector;
    }
    public void setCollector(CollectorConfig collector) {
        this.collector = collector;
    }

    public OperatorConfig getOperator() {
        return operator;
    }
    public void setOperator(OperatorConfig operator) {
        this.operator = operator;
    }

}
