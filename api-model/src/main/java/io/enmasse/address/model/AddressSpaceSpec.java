/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.valueextraction.ExtractedValue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.enmasse.admin.model.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

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
public class AddressSpaceSpec extends AbstractWithAdditionalProperties {

    @NotEmpty
    private String type;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<@Valid EndpointSpec> endpoints = Collections.emptyList();
    @Valid
    private NetworkPolicy networkPolicy;
    @NotEmpty
    private String plan;
    @Valid
    private AuthenticationService authenticationService;

    private List<@Valid AddressSpaceSpecConnector> connectors;

    public AddressSpaceSpec() {
    }

    public void setEndpoints(List<EndpointSpec> endpointList) {
        this.endpoints = endpointList;
    }

    public List<EndpointSpec> getEndpoints() {
        return Collections.unmodifiableList(endpoints);
    }

    public void setType(String typeName) {
        this.type = typeName;
    }

    public String getType() {
        return type;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getPlan() {
        return plan;
    }

    public void setNetworkPolicy(NetworkPolicy networkPolicy) {
        this.networkPolicy = networkPolicy;
    }

    public NetworkPolicy getNetworkPolicy() {
        return networkPolicy;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public List<AddressSpaceSpecConnector> getConnectors() {
        return connectors;
    }

    public void setConnectors(List<AddressSpaceSpecConnector> connectors) {
        this.connectors = connectors;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");

        sb
                .append("type=").append(type).append(",")
                .append("plan=").append(plan).append(",")
                .append("authenticationService=").append(authenticationService).append(",")
                .append("endpoints=").append(endpoints).append(",")
                .append("networkPolicy=").append(networkPolicy).append(",")
                .append("connectors=").append(connectors);

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSpec that = (AddressSpaceSpec) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(endpoints, that.endpoints) &&
                Objects.equals(networkPolicy, that.networkPolicy) &&
                Objects.equals(plan, that.plan) &&
                Objects.equals(authenticationService, that.authenticationService) &&
                Objects.equals(connectors, that.connectors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, endpoints, networkPolicy, plan, authenticationService, connectors);
    }
}
