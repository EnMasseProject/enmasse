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
