/*
 * Copyright 2019, EnMasse authors.
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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecretCertificatesStrategy {

    private String caSecretName;
    private Map<String, String> serviceSecretNames;

    public String getCaSecretName() {
        return caSecretName;
    }

    public void setCaSecretName(String caSecretName) {
        this.caSecretName = caSecretName;
    }

    public Map<String, String> getServiceSecretNames() {
        return serviceSecretNames;
    }

    public void setServiceSecretNames(Map<String, String> serviceSecretNames) {
        this.serviceSecretNames = serviceSecretNames;
    }

}
