/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.enmasse.model.validation.AddressForwarderName;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractWithAdditionalProperties.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
        )
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AddressForwarderName
public class AddressSpecForwarder extends AbstractWithAdditionalProperties {
    @NotNull
    private String name;
    @NotNull
    @NotEmpty
    private String remoteAddress;
    @NotNull
    private AddressSpecForwarderDirection direction;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public AddressSpecForwarderDirection getDirection() {
        return direction;
    }

    public void setDirection(AddressSpecForwarderDirection direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "AddressSpecForwarder{" +
                "name='" + name + '\'' +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", direction=" + direction +
                '}';
    }
}
