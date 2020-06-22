/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config.compat;

import java.util.List;
import java.util.Optional;

import org.eclipse.hono.config.FileFormat;

public class AbstractConfig {

     public Optional<String> trustStorePath;
     public Optional<String> trustStorePassword;
     public Optional<String> pathSeparator;
     public Optional<String> keyStorePath;
     public Optional<String> keyStorePassword;
     public Optional<String> certPath;
     public Optional<String> keyPath;
     public Optional<String> trustStoreFormat;
     public Optional<String> keyFormat;
     public Optional<List<String>> secureProtocols;

    public void applyTo(org.eclipse.hono.config.AbstractConfig result) {
        this.trustStorePath.ifPresent(result::setTrustStorePath);
        this.trustStorePassword.ifPresent(result::setTrustStorePassword);
        this.pathSeparator.ifPresent(result::setPathSeparator);
        this.keyStorePath.ifPresent(result::setKeyStorePath);
        this.keyStorePassword.ifPresent(result::setKeyStorePassword);
        this.certPath.ifPresent(result::setCertPath);
        this.keyPath.ifPresent(result::setKeyPath);
        this.trustStoreFormat.map(FileFormat::valueOf).ifPresent(result::setTrustStoreFormat);
        this.keyFormat.map(FileFormat::valueOf).ifPresent(result::setKeyFormat);
        this.secureProtocols.ifPresent(result::setSecureProtocols);
    }

}
