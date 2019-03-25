/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import java.util.Map;

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
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IoTConfigStatus {

    private boolean initialized = false;
    private String state;
    private String authenticationServicePSK;
    private Map<String, AdapterStatus> adapters;

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAuthenticationServicePSK() {
        return authenticationServicePSK;
    }

    public void setAuthenticationServicePSK(String authenticationServicePSK) {
        this.authenticationServicePSK = authenticationServicePSK;
    }

    public Map<String, AdapterStatus> getAdapters() {
        return adapters;
    }

    public void setAdapters(Map<String, AdapterStatus> adapters) {
        this.adapters = adapters;
    }

}
