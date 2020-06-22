/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config.compat;

import java.util.List;
import java.util.Optional;

import org.eclipse.hono.service.auth.delegating.AuthenticationServerClientConfigProperties;

public class AuthenticationServerClientConfig extends ClientConfig {

    public Optional<List<String>> supportedSaslMechanisms;
    public SignatureSupportingConfig validation;

    public void applyTo(final AuthenticationServerClientConfigProperties result) {
        super.applyTo(result);

        this.supportedSaslMechanisms.ifPresent(result::setSupportedSaslMechanisms);
        this.validation.applyTo(result.getValidation());
    }

    public AuthenticationServerClientConfigProperties toHono() {
        var result = new AuthenticationServerClientConfigProperties();
        applyTo(result);
        return result;
    }
}
