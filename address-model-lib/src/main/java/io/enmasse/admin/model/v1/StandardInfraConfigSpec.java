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
@JsonPropertyOrder({"version", "admin", "broker", "router"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfigSpec {

    private String version;
    private StandardInfraConfigSpecAdmin admin;
    private StandardInfraConfigSpecBroker broker;
    private StandardInfraConfigSpecRouter router;

    public StandardInfraConfigSpec() { }

    public StandardInfraConfigSpec(String version, StandardInfraConfigSpecAdmin admin, StandardInfraConfigSpecBroker broker, StandardInfraConfigSpecRouter router) {
        this.version = version;
        this.admin = admin;
        this.broker = broker;
        this.router = router;
    }

    private Map<String, Object> additionalProperties = new HashMap<>(0);

    public String getVersion() {
        return version;
    }

    public StandardInfraConfigSpecAdmin getAdmin() {
        return admin;
    }

    public StandardInfraConfigSpecBroker getBroker() {
        return broker;
    }

    public StandardInfraConfigSpecRouter getRouter() {
        return router;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfigSpec that = (StandardInfraConfigSpec) o;
        return Objects.equals(version, that.version) &&
                Objects.equals(admin, that.admin) &&
                Objects.equals(broker, that.broker) &&
                Objects.equals(router, that.router);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, admin, broker, router);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setAdmin(StandardInfraConfigSpecAdmin admin) {
        this.admin = admin;
    }

    public void setBroker(StandardInfraConfigSpecBroker broker) {
        this.broker = broker;
    }

    public void setRouter(StandardInfraConfigSpecRouter router) {
        this.router = router;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
