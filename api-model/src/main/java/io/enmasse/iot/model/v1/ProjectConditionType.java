/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProjectConditionType {
    READY("Ready"),
    RESOURCES_CREATED("ResourcesCreated"),
    RESOURCES_READY("ResourcesReady"),
    ;

    @JsonValue
    private final String value;

    private ProjectConditionType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
