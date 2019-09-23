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
public class AdapterConfig extends ServiceConfig {

    private Boolean enabled;
    private EndpointConfig endpoint;
    private CommonAdapterContainers containers;
    private JavaContainerOptions java;

    public Boolean getEnabled() {
        return enabled;
    }
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    public EndpointConfig getEndpoint() {
        return endpoint;
    }
    public void setEndpoint(EndpointConfig endpoint) {
        this.endpoint = endpoint;
    }

    public CommonAdapterContainers getContainers() {
        return containers;
    }
    public void setContainers(CommonAdapterContainers containers) {
        this.containers = containers;
    }

    public void setJava(JavaContainerOptions java) {
        this.java = java;
    }
    public JavaContainerOptions getJava() {
        return java;
    }
}
