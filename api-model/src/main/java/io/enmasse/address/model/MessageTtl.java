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
public class MessageTtl extends AbstractWithAdditionalProperties {
    private Long maximum;
    private Long minimum;

    public Long getMaximum() {
        return maximum;
    }

    public void setMaximum(Long maximum) {
        this.maximum = maximum;
    }

    public Long getMinimum() {
        return minimum;
    }

    public void setMinimum(Long minimum) {
        this.minimum = minimum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageTtl messageTtl = (MessageTtl) o;
        return Objects.equals(maximum, messageTtl.maximum) &&
                Objects.equals(minimum, messageTtl.minimum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maximum, minimum);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TtlSpec{");
        sb.append("maximum=").append(maximum);
        sb.append(", minimum=").append(minimum);
        sb.append('}');
        return sb.toString();
    }
}
