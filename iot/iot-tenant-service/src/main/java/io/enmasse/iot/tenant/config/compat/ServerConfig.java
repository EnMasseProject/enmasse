/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config.compat;

import java.util.Optional;

public class ServerConfig extends AbstractConfig {

    public Optional<Integer> port;
    public Optional<String> bindAddress;
    public Optional<Boolean> nativeTlsRequired;
    public Optional<Boolean> insecurePortEnabled;
    public Optional<String> insecurePortBindAddress;
    public Optional<Integer> insecurePort;
    public Optional<Boolean> sni;

    public void applyTo(org.eclipse.hono.config.ServerConfig result) {
        super.applyTo(result);

        this.port.ifPresent(result::setPort);
        this.bindAddress.ifPresent(result::setBindAddress);
        this.nativeTlsRequired.ifPresent(result::setNativeTlsRequired);
        this.insecurePortEnabled.ifPresent(result::setInsecurePortEnabled);
        this.insecurePortBindAddress.ifPresent(result::setInsecurePortBindAddress);
        this.insecurePort.ifPresent(result::setInsecurePort);
        this.sni.ifPresent(result::setSni);
    }

}
