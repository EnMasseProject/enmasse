/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.Objects;

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
public class SubscriptionStatus extends AbstractWithAdditionalProperties {
        private Integer maxConsumers;

        public Integer getMaxConsumers() {
                return maxConsumers;
        }

        public void setMaxConsumers(Integer maxConsumers) {
                this.maxConsumers = maxConsumers;
        }

        @Override
        public String toString() {
                return "SubscriptionStatus{" +
                        "maxConsumers=" + maxConsumers +
                        '}';
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                SubscriptionStatus that = (SubscriptionStatus) o;
                return Objects.equals(maxConsumers, that.maxConsumers);
        }

        @Override
        public int hashCode() {
                return Objects.hash(maxConsumers);
        }
}
