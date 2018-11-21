/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyEgressRule;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyPeer;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"ingress", "egress"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkPolicy {
    private final NetworkPolicyIngressRule ingress;
    private final NetworkPolicyEgressRule egress;

    private Map<String, Object> additionalProperties = new HashMap<>(0);

    public NetworkPolicy(@JsonProperty("ingress") NetworkPolicyIngressRule ingress,
                         @JsonProperty("egress") NetworkPolicyEgressRule egress) {
        this.ingress = ingress;
        this.egress = egress;
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

    public NetworkPolicyIngressRule getIngress() {
        return ingress;
    }

    public NetworkPolicyEgressRule getEgress() {
        return egress;
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
