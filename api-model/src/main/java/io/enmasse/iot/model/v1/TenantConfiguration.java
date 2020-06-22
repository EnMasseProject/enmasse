/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.Doneable;
import io.quarkus.runtime.annotations.RegisterForReflection;
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
@RegisterForReflection
public class TenantConfiguration {

    private Boolean enabled;

    private long minimumMessageSize;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Map<String, AdapterConfiguration> adapters;

    private Map<String,Object> defaults;
    private Map<String,Object> extensions;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<TrustAnchor> trustAnchors;

    private ResourceLimits resourceLimits;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public long getMinimumMessageSize() {
        return minimumMessageSize;
    }

    public void setMinimumMessageSize(long minimumMessageSize) {
        this.minimumMessageSize = minimumMessageSize;
    }

    public Map<String, AdapterConfiguration> getAdapters() {
        return adapters;
    }

    public void setAdapters(Map<String, AdapterConfiguration> adapters) {
        this.adapters = adapters;
    }

    public Map<String,Object> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String,Object> defaults) {
        this.defaults = defaults;
    }

    public Map<String,Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String,Object> extensions) {
        this.extensions = extensions;
    }

    public List<TrustAnchor> getTrustAnchors() {
        return trustAnchors;
    }

    public void setTrustAnchors(List<TrustAnchor> trustAnchors) {
        this.trustAnchors = trustAnchors;
    }

    public ResourceLimits getResourceLimits() {
        return resourceLimits;
    }

    public void setResourceLimits(ResourceLimits resourceLimits) {
        this.resourceLimits = resourceLimits;
    }

}
