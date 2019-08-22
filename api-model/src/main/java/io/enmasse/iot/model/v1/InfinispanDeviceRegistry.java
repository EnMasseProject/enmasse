/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
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
public class InfinispanDeviceRegistry {

    private ContainerConfig container;
    private InfinispanServer server;

    public ContainerConfig getContainer() {
        return container;
    }
    public void setContainer(ContainerConfig container) {
        this.container = container;
    }

    public InfinispanServer getServer() {
        return server;
    }
    public void setServer(InfinispanServer server) {
        this.server = server;
    }
}
