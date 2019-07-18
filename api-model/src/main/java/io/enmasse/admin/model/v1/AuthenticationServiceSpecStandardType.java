/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AuthenticationServiceSpecStandardType {
    @JsonProperty("ephemeral")
    ephemeral,
    @JsonProperty("persistent-claim")
    persistent_claim;
}
