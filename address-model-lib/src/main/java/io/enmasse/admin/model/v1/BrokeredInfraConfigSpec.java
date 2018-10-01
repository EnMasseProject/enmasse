/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.*;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"version", "admin", "broker"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrokeredInfraConfigSpec {

    private String version;
    private BrokeredInfraConfigSpecAdmin admin;
    private BrokeredInfraConfigSpecBroker broker;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokeredInfraConfigSpec that = (BrokeredInfraConfigSpec) o;
        return Objects.equals(version, that.version) &&
                Objects.equals(admin, that.admin) &&
                Objects.equals(broker, that.broker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, admin, broker);
    }

    public BrokeredInfraConfigSpecAdmin getAdmin() {
        return admin;
    }

    public BrokeredInfraConfigSpecBroker getBroker() {
        return broker;
    }

    public String getVersion() {
        return version;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setAdmin(BrokeredInfraConfigSpecAdmin admin) {
        this.admin = admin;
    }

    public void setBroker(BrokeredInfraConfigSpecBroker broker) {
        this.broker = broker;
    }
}
