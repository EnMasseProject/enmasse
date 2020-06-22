/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(NON_NULL)
@RegisterForReflection
public class CommonServiceConfig extends ServiceConfig {

    private JavaContainerConfig container;
    private TlsOptions tls;

    public JavaContainerConfig getContainer() {
        return container;
    }
    public void setContainer(JavaContainerConfig container) {
        this.container = container;
    }

    public void setTls(TlsOptions tls) {
        this.tls = tls;
    }
    public TlsOptions getTls() {
        return tls;
    }
}
