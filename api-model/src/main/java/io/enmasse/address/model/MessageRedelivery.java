/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.sundr.builder.annotations.Buildable;

import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageRedelivery extends AbstractWithAdditionalProperties {

    private Integer maximumDeliveryAttempts;
    private Long redeliveryDelay;
    private Double redeliveryDelayMultiplier;
    private Long maximumDeliveryDelay;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageRedelivery messageTtl = (MessageRedelivery) o;
        return Objects.equals(maximumDeliveryAttempts, messageTtl.maximumDeliveryAttempts) &&
               Objects.equals(redeliveryDelay, messageTtl.redeliveryDelay) &&
               Objects.equals(redeliveryDelayMultiplier, messageTtl.redeliveryDelayMultiplier) &&
                Objects.equals(maximumDeliveryDelay, messageTtl.maximumDeliveryDelay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maximumDeliveryAttempts, redeliveryDelay, redeliveryDelayMultiplier, maximumDeliveryDelay);
    }

    public Integer getMaximumDeliveryAttempts() {
        return maximumDeliveryAttempts;
    }

    public void setMaximumDeliveryAttempts(Integer maximumDeliveryAttempts) {
        this.maximumDeliveryAttempts = maximumDeliveryAttempts;
    }

    public Long getRedeliveryDelay() {
        return redeliveryDelay;
    }

    public void setRedeliveryDelay(Long redeliveryDelay) {
        this.redeliveryDelay = redeliveryDelay;
    }

    public Double getRedeliveryDelayMultiplier() {
        return redeliveryDelayMultiplier;
    }

    public void setRedeliveryDelayMultiplier(Double redeliveryDelayMultiplier) {
        this.redeliveryDelayMultiplier = redeliveryDelayMultiplier;
    }

    public Long getMaximumDeliveryDelay() {
        return maximumDeliveryDelay;
    }

    public void setMaximumDeliveryDelay(Long maximumDeliveryDelay) {
        this.maximumDeliveryDelay = maximumDeliveryDelay;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageRedelivery{");
        sb.append("maximumDeliveryAttempts=").append(maximumDeliveryAttempts);
        sb.append(", redeliveryDelay=").append(redeliveryDelay);
        sb.append(", redeliveryDelayMultiplier=").append(redeliveryDelayMultiplier);
        sb.append(", maximumDeliveryDelay=").append(maximumDeliveryDelay);
        sb.append('}');
        return sb.toString();
    }

}
