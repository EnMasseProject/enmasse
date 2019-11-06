/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import javax.validation.constraints.NotNull;
import java.util.Objects;

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
public class AddressSpaceSpecConnectorEndpoint extends AbstractWithAdditionalProperties {
    @NotNull
    private String host;

    private Integer port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "AddressSpaceSpecConnectorEndpoint{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSpecConnectorEndpoint endpoint = (AddressSpaceSpecConnectorEndpoint) o;
        return Objects.equals(host, endpoint.host) &&
                Objects.equals(port, endpoint.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
