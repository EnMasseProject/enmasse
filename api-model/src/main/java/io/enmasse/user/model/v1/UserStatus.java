/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.address.model.Phase;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.sundr.builder.annotations.Buildable;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserStatus extends AbstractWithAdditionalProperties {
        private Phase phase;
        private String message;
        private Long generation;

        public Phase getPhase() {
                return phase;
        }

        public void setPhase(Phase phase) {
                this.phase = phase;
        }

        public String getMessage() {
                return message;
        }

        public void setMessage(String message) {
                this.message = message;
        }

        public Long getGeneration() {
                return generation;
        }

        public void setGeneration(Long generation) {
                this.generation = generation;
        }

        @Override
        public String toString() {
                return "UserStatus{" +
                        "phase=" + phase +
                        ", message='" + message + '\'' +
                        ", generation=" + generation +
                        '}';
        }
}
