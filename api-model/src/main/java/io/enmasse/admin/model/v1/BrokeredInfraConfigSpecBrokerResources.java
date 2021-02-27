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
        builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonPropertyOrder({"cpu", "memory", "storage"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrokeredInfraConfigSpecBrokerResources extends AbstractWithAdditionalProperties {
    private String memory;
    private String storage;
    private String cpu;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokeredInfraConfigSpecBrokerResources that = (BrokeredInfraConfigSpecBrokerResources) o;
        return Objects.equals(memory, that.memory) &&
                Objects.equals(storage, that.storage) &&
                Objects.equals(cpu, that.cpu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memory, storage, cpu);
    }

    @Override
    public String toString() {
        return "BrokeredInfraConfigSpecBrokerResources{" +
                "memory='" + memory + '\'' +
                ", storage='" + storage + '\'' +
                ", cpu='" + cpu + '\'' +
                '}';
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getMemory() {
        return memory;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getStorage() {
        return storage;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

}
