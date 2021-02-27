/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.admin.model.AddressSpacePlan;
import io.enmasse.admin.model.v1.InfraConfig;
import io.sundr.builder.annotations.Buildable;

import java.util.*;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the Standard address space type.
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceType {
    private String name;
    private String description;
    private List<@Valid AddressSpacePlan> plans = new ArrayList<>();
    private List<@Valid AddressType> addressTypes = new ArrayList<>();
    private List<@Valid EndpointSpec> availableEndpoints = new ArrayList<>();
    private List<@Valid InfraConfig> infraConfigs = new ArrayList<>();
    private List<@Valid RouteServicePortDescription> routeServicePorts = new ArrayList<>();
    private List<@Valid CertificateProviderTypeDescription> certificateProviderTypes = new ArrayList<>();
    private List<@Valid EndpointExposeTypeDescription> endpointExposeTypes = new ArrayList<>();

    public AddressSpaceType() {
    }

    public AddressSpaceType(String name, String description, List<AddressSpacePlan> plans, List<AddressType> addressTypes, List<EndpointSpec> availableEndpoints, List<InfraConfig> infraConfigs,
                            List<RouteServicePortDescription> routeServicePorts, List<CertificateProviderTypeDescription> certificateProviderTypes, List<EndpointExposeTypeDescription> endpointExposeTypes) {
        this.name = name;
        this.description = description;
        this.plans = plans;
        this.addressTypes = addressTypes;
        this.availableEndpoints = availableEndpoints;
        this.infraConfigs = infraConfigs;
        this.routeServicePorts = routeServicePorts;
        this.certificateProviderTypes = certificateProviderTypes;
        this.endpointExposeTypes = endpointExposeTypes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setAddressTypes(List<AddressType> addressTypes) {
        this.addressTypes = addressTypes;
    }

    public List<AddressType> getAddressTypes() {
        return Collections.unmodifiableList(addressTypes);
    }

    public void setPlans(List<AddressSpacePlan> plans) {
        this.plans = plans;
    }

    public List<AddressSpacePlan> getPlans() {
        return Collections.unmodifiableList(plans);
    }

    public Optional<AddressSpacePlan> findAddressSpacePlan(String name) {
        for (AddressSpacePlan plan : plans) {
            if (plan.getMetadata().getName().equals(name)) {
                return Optional.of(plan);
            }
        }
        return Optional.empty();
    }

    public Optional<InfraConfig> findInfraConfig(String name) {
        for (InfraConfig infraConfig : infraConfigs) {
            if (name.equals(infraConfig.getMetadata().getName())) {
                return Optional.of(infraConfig);
            }
        }
        return Optional.empty();
    }

    public void setAvailableEndpoints(List<EndpointSpec> availableEndpoints) {
        this.availableEndpoints = availableEndpoints;
    }

    public List<EndpointSpec> getAvailableEndpoints() {
        return Collections.unmodifiableList(availableEndpoints);
    }

    public Optional<AddressType> findAddressType(String type) {
        for (AddressType addressType : addressTypes) {
            if (addressType.getName().equals(type)) {
                return Optional.of(addressType);
            }
        }
        return Optional.empty();
    }

    public void setInfraConfigs(List<InfraConfig> infraConfigs) {
        this.infraConfigs = infraConfigs;
    }

    public List<InfraConfig> getInfraConfigs() {
        return infraConfigs;
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

}
