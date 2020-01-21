/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractWithAdditionalProperties.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AddressSpec extends AbstractWithAdditionalProperties {

    @NotNull
    private String address;
    private String addressSpace;
    @NotNull
    private String type;
    @NotNull
    private String plan;
    private String topic;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<@Valid AddressSpecForwarder> forwarders;

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddressSpace(String addressSpace) {
        this.addressSpace = addressSpace;
    }

    /**
     * @deprecated Use {@link Address#extractAddressSpace(Address)} instead.
     */
    @Deprecated
    public String getAddressSpace() {
        return addressSpace;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getPlan() {
        return plan;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return this.topic;
    }

    public List<AddressSpecForwarder> getForwarders() {
        return forwarders;
    }

    public void setForwarders(List<AddressSpecForwarder> forwarders) {
        this.forwarders = forwarders;
    }

    @Override
    public String toString() {

         final StringBuilder sb = new StringBuilder("{");

         sb.append("address=").append(address);
         sb.append(",type=").append(type);
         sb.append(",plan=").append(plan);
         if (topic != null) {
             sb.append(",topic=").append(topic);
         }
         if (forwarders != null) {
             sb.append(",forwarders=").append(forwarders);
         }
         sb.append("}");

         return sb.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AddressSpec other = (AddressSpec) o;

        return
                Objects.equals(address, other.address) &&
                Objects.equals(addressSpace, other.addressSpace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, addressSpace);
    }
}
