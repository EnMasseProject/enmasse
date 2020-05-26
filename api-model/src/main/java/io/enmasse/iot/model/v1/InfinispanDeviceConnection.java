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
                value = "done"
                )
        )
@JsonInclude(NON_NULL)
public class InfinispanDeviceConnection {

    @JsonUnwrapped
    private CommonDeviceRegistry commonDeviceRegistry;
    private ContainerConfig container;
    private InfinispanDeviceConnectionServer server;

    public CommonDeviceRegistry getCommonDeviceRegistry() {
        return commonDeviceRegistry;
    }
    public void setCommonDeviceRegistry(CommonDeviceRegistry commonDeviceRegistry) {
        this.commonDeviceRegistry = commonDeviceRegistry;
    }

    public ContainerConfig getContainer() {
        return container;
    }
    public void setContainer(ContainerConfig container) {
        this.container = container;
    }

    public InfinispanDeviceConnectionServer getServer() {
        return server;
    }
    public void setServer(InfinispanDeviceConnectionServer server) {
        this.server = server;
    }

}
