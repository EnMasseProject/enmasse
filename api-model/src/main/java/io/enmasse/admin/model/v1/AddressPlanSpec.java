/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import io.enmasse.address.model.MessageRedelivery;
import io.enmasse.address.model.MessageTtl;
import io.sundr.builder.annotations.Buildable;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@JsonPropertyOrder({"displayName", "shortDescription", "addressType", "resources"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressPlanSpec extends AbstractWithAdditionalProperties {
    private String shortDescription;
    private String addressType;
    private Integer partitions;
    private MessageTtl messageTtl;
    private MessageRedelivery messageRedelivery;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Map<String, Double> resources = new HashMap<>();

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public Map<String, Double> getResources() {
        return Collections.unmodifiableMap(resources);
    }

    public void setResources(Map<String, Double> resources) {
        this.resources = new HashMap<>(resources);
    }

    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(Integer partitions) {
        this.partitions = partitions;
    }

    public MessageTtl getMessageTtl() {
        return messageTtl;
    }

    public void setMessageTtl(MessageTtl messageTtl) {
        this.messageTtl = messageTtl;
    }

    public MessageRedelivery getMessageRedelivery() {
        return messageRedelivery;
    }

    public void setMessageRedelivery(MessageRedelivery messageRedelivery) {
        this.messageRedelivery = messageRedelivery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressPlanSpec that = (AddressPlanSpec) o;
        return Objects.equals(shortDescription, that.shortDescription) &&
                Objects.equals(addressType, that.addressType) &&
                Objects.equals(partitions, that.partitions) &&
                Objects.equals(resources, that.resources) &&
                Objects.equals(messageTtl, that.messageTtl) &&
                Objects.equals(messageRedelivery, that.messageRedelivery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortDescription, addressType, partitions, resources);
    }

    @Override
    public String toString() {
        return "AddressPlanSpec{" +
                "shortDescription='" + shortDescription + '\'' +
                ", addressType='" + addressType + '\'' +
                ", partitions='" + partitions + '\'' +
                ", resources=" + resources +
                ", messageTtl=" + messageTtl +
                ", messageRedelivery=" + messageRedelivery +
                '}';
    }
}
