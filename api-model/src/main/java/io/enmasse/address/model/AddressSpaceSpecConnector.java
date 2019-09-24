/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.enmasse.model.validation.AddressSpaceConnectorName;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
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
@AddressSpaceConnectorName
public class AddressSpaceSpecConnector extends AbstractWithAdditionalProperties {
    @NotNull
    private String name;

    @NotNull
    @NotEmpty
    private List<@Valid  AddressSpaceSpecConnectorEndpoint> endpointHosts = Collections.emptyList();

    private AddressSpaceSpecConnectorCredentials credentials;
    private AddressSpaceSpecConnectorTls tls;

    private List<@Valid AddressSpaceSpecConnectorAddressRule> addresses = Collections.emptyList();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AddressSpaceSpecConnectorEndpoint> getEndpointHosts() {
        return endpointHosts;
    }

    public void setEndpointHosts(List<AddressSpaceSpecConnectorEndpoint> endpointHosts) {
        this.endpointHosts = endpointHosts;
    }

    public AddressSpaceSpecConnectorCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(AddressSpaceSpecConnectorCredentials credentials) {
        this.credentials = credentials;
    }

    public AddressSpaceSpecConnectorTls getTls() {
        return tls;
    }

    public void setTls(AddressSpaceSpecConnectorTls tls) {
        this.tls = tls;
    }

    public List<AddressSpaceSpecConnectorAddressRule> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<AddressSpaceSpecConnectorAddressRule> addresses) {
        this.addresses = addresses;
    }

    @JsonIgnore
    public int getPort(Integer port) {
        if (port != null) {
            return port;
        } else if (tls != null) {
            return 5671; // IANA AMQPS
        } else {
            return 5672; // IANA AMQP
        }
    }

    @Override
    public String toString() {
        return "AddressSpaceSpecConnector{" +
                "name='" + name + '\'' +
                ", endpointHosts=" + endpointHosts +
                ", credentials=" + credentials +
                ", tls=" + tls +
                ", addresses=" + addresses +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSpecConnector connector = (AddressSpaceSpecConnector) o;
        return Objects.equals(name, connector.name) &&
                Objects.equals(endpointHosts, connector.endpointHosts) &&
                Objects.equals(credentials, connector.credentials) &&
                Objects.equals(tls, connector.tls) &&
                Objects.equals(addresses, connector.addresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, endpointHosts, credentials, tls, addresses);
    }
}
