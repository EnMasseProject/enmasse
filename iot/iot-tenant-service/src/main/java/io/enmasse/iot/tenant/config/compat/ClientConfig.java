/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config.compat;

import java.util.Optional;

import org.eclipse.hono.config.ClientConfigProperties;

public class ClientConfig extends AuthenticatingClientConfig {

    public Optional<String> amqpHostname;
    public Optional<Integer> connectTimeoutMillis;
    public Optional<Long> flowLatency;
    public Optional<Integer> idleTimeoutMillis;
    public Optional<Integer> initialCredits;
    public Optional<Long> linkEstablishmentTimeout;
    public Optional<String> name;
    public Optional<Integer> reconnectAttempts;
    public Optional<Long> reconnectMinDelayMillis;
    public Optional<Long> reconnectMaxDelayMillis;
    public Optional<Long> reconnectDelayIncrementMillis;
    public Optional<Long> requestTimeoutMillis;
    public Optional<Long> sendMessageTimeoutMillis;

    public void applyTo(ClientConfigProperties result) {
        super.applyTo(result);

        this.amqpHostname.ifPresent(result::setAmqpHostname);
        this.connectTimeoutMillis.ifPresent(result::setConnectTimeout);
        this.flowLatency.ifPresent(result::setFlowLatency);
        this.idleTimeoutMillis.ifPresent(result::setIdleTimeout);
        this.initialCredits.ifPresent(result::setInitialCredits);
        this.linkEstablishmentTimeout.ifPresent(result::setLinkEstablishmentTimeout);
        this.name.ifPresent(result::setName);
        this.reconnectAttempts.ifPresent(result::setReconnectAttempts);
        this.reconnectMinDelayMillis.ifPresent(result::setReconnectMinDelay);
        this.reconnectMaxDelayMillis.ifPresent(result::setReconnectMaxDelay);
        this.reconnectDelayIncrementMillis.ifPresent(result::setReconnectDelayIncrement);
        this.requestTimeoutMillis.ifPresent(result::setRequestTimeout);
        this.sendMessageTimeoutMillis.ifPresent(result::setSendMessageTimeout);
    }

}
