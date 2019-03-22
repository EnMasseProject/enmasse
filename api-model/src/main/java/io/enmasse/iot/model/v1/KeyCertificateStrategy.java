/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import java.nio.ByteBuffer;

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
public class KeyCertificateStrategy {

    private ByteBuffer key;
    private ByteBuffer certificate;

    public ByteBuffer getKey() {
        return key;
    }
    public void setKey(ByteBuffer key) {
        this.key = key;
    }

    public ByteBuffer getCertificate() {
        return certificate;
    }
    public void setCertificate(ByteBuffer certificate) {
        this.certificate = certificate;
    }

}
