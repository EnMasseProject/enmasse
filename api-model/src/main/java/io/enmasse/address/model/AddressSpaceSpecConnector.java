/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.enmasse.model.validation.AddressSpaceConnectorName;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractWithAdditionalProperties.class)}
)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@AddressSpaceConnectorName
public class AddressSpaceSpecConnector extends AbstractWithAdditionalProperties {
    @NotNull
    private String name;

    private String role;
    private Integer maxFrameSize;
    private Integer idleTimeout;

    @NotNull
    @NotEmpty
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<@Valid  AddressSpaceSpecConnectorEndpoint> endpointHosts = Collections.emptyList();

    private AddressSpaceSpecConnectorCredentials credentials;
    private AddressSpaceSpecConnectorTls tls;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
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

    public String getRole() {
        if (role != null) {
            return role;
        } else {
            return "route-container";
        }
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setMaxFrameSize(Integer maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public Integer getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Integer idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    @Override
    public String toString() {
        return "AddressSpaceSpecConnector{" +
                "name='" + name + '\'' +
                ", endpointHosts=" + endpointHosts +
                ", credentials=" + credentials +
                ", tls=" + tls +
                ", addresses=" + addresses +
                ", role=" + role +
                ", idleTimeout=" + idleTimeout +
                ", maxFrameSize=" + maxFrameSize +
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
                Objects.equals(addresses, connector.addresses) &&
                Objects.equals(role, connector.role) &&
                Objects.equals(idleTimeout, connector.idleTimeout) &&
                Objects.equals(maxFrameSize, connector.maxFrameSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, endpointHosts, credentials, tls, addresses, role, idleTimeout, maxFrameSize);
    }
}
