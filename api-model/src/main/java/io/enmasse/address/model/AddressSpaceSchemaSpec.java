/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.sundr.builder.annotations.Buildable;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class AddressSpaceSchemaSpec extends AbstractWithAdditionalProperties {
    private String description;
    private List<@Valid AddressTypeInformation> addressTypes = new ArrayList<>();
    private List<@Valid AddressSpacePlanDescription> plans = new ArrayList<>();
    private List<@Valid String> authenticationServices = new ArrayList<>();
    private List<@Valid RouteServicePortDescription> routeServicePorts = new ArrayList<>();
    private List<@Valid CertificateProviderTypeDescription> certificateProviderTypes = new ArrayList<>();
    private List<@Valid EndpointExposeTypeDescription> endpointExposeTypes = new ArrayList<>();

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<AddressTypeInformation> getAddressTypes() {
        return this.addressTypes;
    }

    public void setAddressTypes(final List<AddressTypeInformation> addressTypes) {
        this.addressTypes = addressTypes;
    }

    public List<AddressSpacePlanDescription> getPlans() {
        return this.plans;
    }

    public void setPlans(final List<AddressSpacePlanDescription> plans) {
        this.plans = plans;
    }

    public List<String> getAuthenticationServices() {
        return authenticationServices;
    }

    public void setAuthenticationServices(List<String> authenticationServices) {
        this.authenticationServices = authenticationServices;
    }

    public List<RouteServicePortDescription> getRouteServicePorts() {
        return routeServicePorts;
    }

    public void setRouteServicePorts(List<RouteServicePortDescription> routeServicePorts) {
        this.routeServicePorts = routeServicePorts;
    }

    public List<CertificateProviderTypeDescription> getCertificateProviderTypes() {
        return certificateProviderTypes;
    }

    public void setCertificateProviderTypes(List<CertificateProviderTypeDescription> certificateProviderTypes) {
        this.certificateProviderTypes = certificateProviderTypes;
    }

    public List<EndpointExposeTypeDescription> getEndpointExposeTypes() {
        return endpointExposeTypes;
    }

    public void setEndpointExposeTypes(List<EndpointExposeTypeDescription> endpointExposeTypes) {
        this.endpointExposeTypes = endpointExposeTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSchemaSpec that = (AddressSpaceSchemaSpec) o;
        return Objects.equals(description, that.description) &&
                Objects.equals(addressTypes, that.addressTypes) &&
                Objects.equals(plans, that.plans) &&
                Objects.equals(authenticationServices, that.authenticationServices) &&
                Objects.equals(routeServicePorts, that.routeServicePorts) &&
                Objects.equals(certificateProviderTypes, that.certificateProviderTypes) &&
                Objects.equals(endpointExposeTypes, that.endpointExposeTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, addressTypes, plans, authenticationServices, routeServicePorts, certificateProviderTypes, endpointExposeTypes);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("description=").append(this.description);
        sb.append(",");
        sb.append("plans=").append(this.plans);
        sb.append(",");
        sb.append("addressTypes=").append(this.addressTypes);
        sb.append("authenticationServices=").append(this.authenticationServices);
        sb.append("routeServicePortDescriptions=").append(this.routeServicePorts);
        sb.append("certificateProviderTypeDescriptions=").append(this.certificateProviderTypes);
        sb.append("endpointExposeTypeDescriptions=").append(this.endpointExposeTypes);
        return sb.append("}").toString();
    }
}
