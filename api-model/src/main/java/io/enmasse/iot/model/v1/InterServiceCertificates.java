/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

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
public class InterServiceCertificates {

    private SecretCertificatesStrategy secretCertificatesStrategy;
    private ServiceCAStrategy serviceCAStrategy;

    public SecretCertificatesStrategy getSecretCertificatesStrategy() {
        return secretCertificatesStrategy;
    }

    public void setSecretCertificatesStrategy(SecretCertificatesStrategy secretCertificatesStrategy) {
        this.secretCertificatesStrategy = secretCertificatesStrategy;
    }

    public ServiceCAStrategy getServiceCAStrategy() {
        return serviceCAStrategy;
    }

    public void setServiceCAStrategy(ServiceCAStrategy serviceCAStrategy) {
        this.serviceCAStrategy = serviceCAStrategy;
    }

}
