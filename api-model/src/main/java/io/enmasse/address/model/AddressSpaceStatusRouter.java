/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

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
public class AddressSpaceStatusRouter extends AbstractWithAdditionalProperties {
        @NotNull
        private String id;

        private Long undelivered;

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        private List<String> neighbors = new ArrayList<>();

        public String getId() {
                return id;
        }

        public void setId(String id) {
                this.id = id;
        }

        public Long getUndelivered() {
                return undelivered;
        }

        public void setUndelivered(Long undelivered) {
                this.undelivered = undelivered;
        }

        public List<String> getNeighbors() {
                return neighbors;
        }

        public void setNeighbors(List<String> neighbors) {
                this.neighbors = neighbors;
        }

        @Override
        public String toString() {
                return "AddressSpaceStatusRouter{" +
                        "id='" + id + '\'' +
                        ", undelivered=" + undelivered +
                        ", neighbors=" + neighbors +
                        '}';
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                AddressSpaceStatusRouter that = (AddressSpaceStatusRouter) o;
                return Objects.equals(id, that.id) &&
                        Objects.equals(undelivered, that.undelivered) &&
                        Objects.equals(neighbors, that.neighbors);
        }

        @Override
        public int hashCode() {
                return Objects.hash(id, undelivered, neighbors);
        }
}
