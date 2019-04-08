/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.api.model.Doneable;
import io.strimzi.api.kafka.model.KafkaSpec;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"version", "networkPolicy", "kafka"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaInfraConfigSpec extends AbstractWithAdditionalProperties {

    private String version;
    private NetworkPolicy networkPolicy;
    private KafkaSpec kafka;

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaInfraConfigSpec that = (KafkaInfraConfigSpec) o;
        return Objects.equals(version, that.version) &&
                Objects.equals(networkPolicy, that.networkPolicy) &&
                Objects.equals(kafka, that.kafka);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, networkPolicy, kafka);
    }

    @Override
    public String toString() {
        return "StandardInfraConfigSpec{" +
                "version='" + version + '\'' +
                ", networkPolicy=" + networkPolicy +
                ", kafka=" + kafka +
                '}';
    }

    public void setNetworkPolicy(NetworkPolicy networkPolicy) {
        this.networkPolicy = networkPolicy;
    }

    public NetworkPolicy getNetworkPolicy() {
        return networkPolicy;
    }

    public KafkaSpec getKafka() {
        return kafka;
    }

    public void setKafka(KafkaSpec kafka) {
        this.kafka = kafka;
    }
}
