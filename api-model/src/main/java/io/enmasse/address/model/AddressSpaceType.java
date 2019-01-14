/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.InfraConfig;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.*;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the Standard address space type.
 */
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
public class AddressSpaceType {
    private String name;
    private String description;
    private InfraConfigDeserializer infraConfigDeserializer;
    private List<@Valid AddressSpacePlan> plans = new ArrayList<>();
    private List<@Valid AddressType> addressTypes = new ArrayList<>();
    private List<@Valid EndpointSpec> availableEndpoints = new ArrayList<>();
    private List<@Valid InfraConfig> infraConfigs = new ArrayList<>();

    public AddressSpaceType() {
    }

    public AddressSpaceType(String name, String description, InfraConfigDeserializer infraConfigDeserializer, List<AddressSpacePlan> plans, List<AddressType> addressTypes, List<EndpointSpec> availableEndpoints, List<InfraConfig> infraConfigs) {
        this.name = name;
        this.description = description;
        this.infraConfigDeserializer = infraConfigDeserializer;
        this.plans = plans;
        this.addressTypes = addressTypes;
        this.availableEndpoints = availableEndpoints;
        this.infraConfigs = infraConfigs;
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

    public void setInfraConfigDeserializer(InfraConfigDeserializer infraConfigDeserializer) {
        this.infraConfigDeserializer = infraConfigDeserializer;
    }

    public InfraConfigDeserializer getInfraConfigDeserializer() {
        return infraConfigDeserializer;
    }
}
