/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.utils;

import java.util.Set;

import io.vertx.core.net.TCPSSLOptions;

public final class VertxUtils {

    private VertxUtils() {}

    public static void applyTlsVersions(final TCPSSLOptions options, final Set<String> tlsVersions) {
        if (tlsVersions != null && !tlsVersions.isEmpty()) {
            // remove all current versions
            options.getEnabledSecureTransportProtocols().forEach(options::removeEnabledSecureTransportProtocol);
            // set new versions
            tlsVersions.forEach(options::addEnabledSecureTransportProtocol);
        }
    }

}
