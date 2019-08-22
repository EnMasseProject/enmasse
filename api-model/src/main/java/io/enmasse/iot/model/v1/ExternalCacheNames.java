/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

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
@JsonInclude(NON_NULL)
public class ExternalCacheNames {

    private String adapterCredentialsCacheName;
    private String devicesCacheName;
    private String deviceStatesCacheName;

    public String getAdapterCredentialsCacheName() {
        return adapterCredentialsCacheName;
    }

    public void setAdapterCredentialsCacheName(String adapterCredentialsCacheName) {
        this.adapterCredentialsCacheName = adapterCredentialsCacheName;
    }

    public String getDevicesCacheName() {
        return devicesCacheName;
    }

    public void setDevicesCacheName(String devicesCacheName) {
        this.devicesCacheName = devicesCacheName;
    }

    public String getDeviceStatesCacheName() {
        return deviceStatesCacheName;
    }

    public void setDeviceStatesCacheName(String deviceStatesCacheName) {
        this.deviceStatesCacheName = deviceStatesCacheName;
    }

}
