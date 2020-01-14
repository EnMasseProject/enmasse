/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class AdaptersConfig {
    private AdapterOptions defaults;
    private AdapterConfig http;
    private AdapterConfig mqtt;
    private AdapterConfig sigfox;
    @JsonProperty("lorawan")
    private AdapterConfig loraWan;

    public AdapterOptions getDefaults() {
        return defaults;
    }
    public void setDefaults(AdapterOptions defaults) {
        this.defaults = defaults;
    }

    public AdapterConfig getHttp() {
        return http;
    }
    public void setHttp(AdapterConfig http) {
        this.http = http;
    }

    public AdapterConfig getMqtt() {
        return mqtt;
    }
    public void setMqtt(AdapterConfig mqtt) {
        this.mqtt = mqtt;
    }

    public AdapterConfig getSigfox() {
        return sigfox;
    }
    public void setSigfox(AdapterConfig sigfox) {
        this.sigfox = sigfox;
    }

    public AdapterConfig getLoraWan() {
        return loraWan;
    }
    public void setLoraWan(AdapterConfig loraWan) {
        this.loraWan = loraWan;
    }
}
