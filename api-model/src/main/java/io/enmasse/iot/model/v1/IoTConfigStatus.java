/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import java.util.List;
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

    private String phase;
    private String phaseReason;
    private String authenticationServicePSK;
    private Map<String, AdapterStatus> adapters;
    private Map<String, ServiceStatus> services;
    private List<ConfigCondition> conditions;

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getPhaseReason() {
        return phaseReason;
    }

    public void setPhaseReason(String phaseReason) {
        this.phaseReason = phaseReason;
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

    public Map<String, ServiceStatus> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceStatus> services) {
        this.services = services;
    }

    public List<ConfigCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConfigCondition> conditions) {
        this.conditions = conditions;
    }

}
