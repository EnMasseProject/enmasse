/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyEgressRule;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyIngressRule;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractWithAdditionalProperties.class)},
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"ingress", "egress"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkPolicy extends AbstractWithAdditionalProperties {
    private final List<NetworkPolicyIngressRule> ingress;
    private final List<NetworkPolicyEgressRule> egress;

    public NetworkPolicy(@JsonProperty("ingress") List<NetworkPolicyIngressRule> ingress,
                         @JsonProperty("egress") List<NetworkPolicyEgressRule> egress) {
        this.ingress = ingress != null ? ingress : new ArrayList<>();
        this.egress = egress != null ? egress : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkPolicy that = (NetworkPolicy) o;
        return Objects.equals(ingress, that.ingress) &&
                Objects.equals(egress, that.egress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingress, egress);
    }

    public List<NetworkPolicyIngressRule> getIngress() {
        return ingress;
    }

    public List<NetworkPolicyEgressRule> getEgress() {
        return egress;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ingress=").append(ingress).append(",")
                .append("egress=").append(egress).append("}");
        return sb.toString();
    }
}
