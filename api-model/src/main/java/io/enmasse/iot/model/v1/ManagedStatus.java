/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedStatus {

    private String passwordTime;
    private String addressSpace;

    public String getPasswordTime() {
        return passwordTime;
    }

    public void setPasswordTime(String passwordTime) {
        this.passwordTime = passwordTime;
    }

    public String getAddressSpace() {
        return addressSpace;
    }

    public void setAddressSpace(String addressSpace) {
        this.addressSpace = addressSpace;
    }

}
