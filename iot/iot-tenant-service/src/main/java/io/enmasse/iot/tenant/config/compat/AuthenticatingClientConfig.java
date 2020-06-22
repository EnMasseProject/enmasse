/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config.compat;

import java.util.Optional;

import org.eclipse.hono.config.AuthenticatingClientConfigProperties;

public class AuthenticatingClientConfig extends AbstractConfig {

    public Optional<String> credentialsPath;
    public Optional<String> host;
    public Optional<Boolean> hostnameVerificationRequired;
    public Optional<String> password;
    public Optional<Integer> port;
    public Optional<String> serverRole;
    public Optional<Boolean> tlsEnabled;
    public Optional<String> username;

    public void applyTo(final AuthenticatingClientConfigProperties result) {
        super.applyTo(result);

        this.credentialsPath.ifPresent(result::setCredentialsPath);
        this.host.ifPresent(result::setHost);
        this.hostnameVerificationRequired.ifPresent(result::setHostnameVerificationRequired);
        this.password.ifPresent(result::setPassword);
        this.port.ifPresent(result::setPort);
        this.serverRole.ifPresent(result::setServerRole);
        this.tlsEnabled.ifPresent(result::setTlsEnabled);
        this.username.ifPresent(result::setUsername);

    }
}
