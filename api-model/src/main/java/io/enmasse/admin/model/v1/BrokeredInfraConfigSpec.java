/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.sundr.builder.annotations.Buildable;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@JsonPropertyOrder({"version", "admin", "broker"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrokeredInfraConfigSpec extends AbstractWithAdditionalProperties {

    private String version;
    private NetworkPolicy networkPolicy;
    private BrokeredInfraConfigSpecAdmin admin;
    private BrokeredInfraConfigSpecBroker broker;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokeredInfraConfigSpec that = (BrokeredInfraConfigSpec) o;
        return Objects.equals(version, that.version) &&
                Objects.equals(networkPolicy, that.networkPolicy) &&
                Objects.equals(admin, that.admin) &&
                Objects.equals(broker, that.broker);
    }

    @Override
    public String toString() {
        return "BrokeredInfraConfigSpec{" +
                "version='" + version + '\'' +
                ", networkPolicy=" + networkPolicy +
                ", admin=" + admin +
                ", broker=" + broker +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, networkPolicy, admin, broker);
    }

    public void setAdmin(BrokeredInfraConfigSpecAdmin admin) {
        this.admin = admin;
    }

    public BrokeredInfraConfigSpecAdmin getAdmin() {
        return admin;
    }

    public void setBroker(BrokeredInfraConfigSpecBroker broker) {
        this.broker = broker;
    }

    public BrokeredInfraConfigSpecBroker getBroker() {
        return broker;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setNetworkPolicy(NetworkPolicy networkPolicy) {
        this.networkPolicy = networkPolicy;
    }

    public NetworkPolicy getNetworkPolicy() {
        return networkPolicy;
    }
}
