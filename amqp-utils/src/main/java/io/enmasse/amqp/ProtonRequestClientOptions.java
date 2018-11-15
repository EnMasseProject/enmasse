/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import org.apache.qpid.proton.engine.SslDomain;

/**
 * TODO: Description
 */
public class ProtonRequestClientOptions {
    private boolean sslEnabled = false;
    private SslDomain sslDomain;
    private boolean saslEnabled = false;
    private String[] saslMechanisms;
    private String containerId = "proton-request-client";

    public String getContainerId() {
        return containerId;
    }

    public ProtonRequestClientOptions setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
        return this;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public SslDomain getSslDomain() {
        return sslDomain;
    }

    public ProtonRequestClientOptions setSslDomain(SslDomain sslDomain) {
        this.sslDomain = sslDomain;
        return this;
    }

    public String[] getSaslMechanisms() {
        return saslMechanisms;
    }

    public ProtonRequestClientOptions setSaslMechanisms(String[] saslMechanisms) {
        this.saslMechanisms = saslMechanisms;
        return this;
    }

    public boolean isSaslEnabled() {
        return saslEnabled;
    }

    public ProtonRequestClientOptions setSaslEnabled(boolean saslEnabled) {
        this.saslEnabled = saslEnabled;
        return this;
    }

    public ProtonRequestClientOptions setContainerId(String containerId) {
        this.containerId = containerId;
        return this;
    }
}
