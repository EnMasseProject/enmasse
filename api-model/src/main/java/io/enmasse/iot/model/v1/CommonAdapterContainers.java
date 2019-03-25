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
public class CommonAdapterContainers {

    private ContainerConfig adapter;
    private ContainerConfig proxy;
    private ContainerConfig proxyConfigurator;

    public ContainerConfig getAdapter() {
        return adapter;
    }
    public void setAdapter(ContainerConfig adapter) {
        this.adapter = adapter;
    }

    public ContainerConfig getProxy() {
        return proxy;
    }
    public void setProxy(ContainerConfig proxy) {
        this.proxy = proxy;
    }

    public ContainerConfig getProxyConfigurator() {
        return proxyConfigurator;
    }
    public void setProxyConfigurator(ContainerConfig proxyConfigurator) {
        this.proxyConfigurator = proxyConfigurator;
    }
}
