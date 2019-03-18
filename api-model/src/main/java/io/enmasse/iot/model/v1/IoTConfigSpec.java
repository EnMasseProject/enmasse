/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
//FIXME: implement missing fields and remove ignore annotation
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IoTConfigSpec {

    private Boolean enableDefaultRoutes;
    private Map<String, ImageOverride> imageOverrides;
    private InterServiceCertificates interServiceCertificates;

    public Boolean getEnableDefaultRoutes() {
        return enableDefaultRoutes;
    }

    public void setEnableDefaultRoutes(Boolean enableDefaultRoutes) {
        this.enableDefaultRoutes = enableDefaultRoutes;
    }

    public Map<String, ImageOverride> getImageOverrides() {
        return imageOverrides;
    }

    public void setImageOverrides(Map<String, ImageOverride> imageOverrides) {
        this.imageOverrides = imageOverrides;
    }

    public InterServiceCertificates getInterServiceCertificates() {
        return interServiceCertificates;
    }

    public void setInterServiceCertificates(InterServiceCertificates interServiceCertificates) {
        this.interServiceCertificates = interServiceCertificates;
    }

}
