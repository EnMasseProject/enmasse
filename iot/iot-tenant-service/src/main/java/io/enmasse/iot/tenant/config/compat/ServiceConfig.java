/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config.compat;

import java.util.Optional;

import org.eclipse.hono.config.ServiceConfigProperties;

public class ServiceConfig extends ServerConfig {

    public Optional<Boolean> singleTenant;
    public Optional<Boolean> networkDebugLogging;
    public Optional<Boolean> waitForDownstreamConnection;
    public Optional<Integer> maxPayloadSize;
    public Optional<Integer> receiverLinkCredit;
    public Optional<String> corsAllowedOrigin;
    public Optional<Long> sendTimeOutInMs;

    public void applyTo(ServiceConfigProperties result) {
        super.applyTo(result);

        this.singleTenant.ifPresent(result::setSingleTenant);
        this.networkDebugLogging.ifPresent(result::setNetworkDebugLoggingEnabled);
        this.waitForDownstreamConnection.ifPresent(result::setWaitForDownstreamConnectionEnabled);
        this.maxPayloadSize.ifPresent(result::setMaxPayloadSize);
        this.receiverLinkCredit.ifPresent(result::setReceiverLinkCredit);
        this.corsAllowedOrigin.ifPresent(result::setCorsAllowedOrigin);
        this.sendTimeOutInMs.ifPresent(result::setSendTimeOut);
    }

    public ServiceConfigProperties toHono() {
        var result = new ServiceConfigProperties();
        applyTo(result);
        return result;
    }

}
