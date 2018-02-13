/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import org.apache.qpid.proton.engine.SslDomain;
import org.apache.qpid.proton.engine.SslPeerDetails;

public class SslOptions {
    private final SslDomain sslDomain;
    private final SslPeerDetails sslPeerDetails;

    public SslOptions(SslDomain sslDomain, SslPeerDetails sslPeerDetails) {
        this.sslDomain = sslDomain;
        this.sslPeerDetails = sslPeerDetails;
    }

    public SslDomain getSslDomain() {
        return sslDomain;
    }

    public SslPeerDetails getSslPeerDetails() {
        return sslPeerDetails;
    }
}
