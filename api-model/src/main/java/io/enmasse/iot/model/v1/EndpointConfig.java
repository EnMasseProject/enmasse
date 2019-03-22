/*
 * Copyright 2019, EnMasse authors.
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
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EndpointConfig {
    private Boolean enableDefaultRoute;

    private SecretNameStrategy secretNameStrategy;
    private KeyCertificateStrategy keyCertificateStrategy;

    public Boolean getEnableDefaultRoute() {
        return enableDefaultRoute;
    }
    public void setEnableDefaultRoute(Boolean enableDefaultRoute) {
        this.enableDefaultRoute = enableDefaultRoute;
    }

    public SecretNameStrategy getSecretNameStrategy() {
        return secretNameStrategy;
    }
    public void setSecretNameStrategy(SecretNameStrategy secretNameStrategy) {
        this.secretNameStrategy = secretNameStrategy;
    }

    public KeyCertificateStrategy getKeyCertificateStrategy() {
        return keyCertificateStrategy;
    }
    public void setKeyCertificateStrategy(KeyCertificateStrategy keyCertificateStrategy) {
        this.keyCertificateStrategy = keyCertificateStrategy;
    }

}
