package enmasse.systemtest.amqp;

import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.engine.SslDomain;
import org.apache.qpid.proton.engine.SslPeerDetails;

/**
 * Options for EnMasse client
 */
public class ClientOptions {
    private final Source source;
    private final Target target;

    public ClientOptions(Source source, Target target) {
        this.source = source;
        this.target = target;
    }

    public Source getSource() {
        return source;
    }

    public boolean useSSL() {
       return  false;
    }

    public SslDomain sslDomain() {
        return null;
    }

    public SslPeerDetails sslPeerDetails() {
        return null;
    }

    public Target getTarget() {
        return target;
    }
}
