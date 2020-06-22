/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config.compat;

import java.util.Optional;

import org.eclipse.hono.config.SignatureSupportingConfigProperties;

public class SignatureSupportingConfig {

    public Optional<String> sharedSecret;
    public Optional<String> keyPath;
    public Optional<Long> tokenExpirationSeconds;
    public Optional<String> certificatePath;

    public SignatureSupportingConfigProperties toHono() {
        var result = new SignatureSupportingConfigProperties();
        applyTo(result);
        return result;
    }

    public void applyTo(SignatureSupportingConfigProperties result) {
        this.sharedSecret.ifPresent(result::setSharedSecret);
        this.keyPath.ifPresent(result::setKeyPath);
        this.tokenExpirationSeconds.ifPresent(result::setTokenExpiration);
        this.certificatePath.ifPresent(result::setCertPath);
    }

}
