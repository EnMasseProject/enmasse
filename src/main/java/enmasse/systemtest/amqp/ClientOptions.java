package enmasse.systemtest.amqp;

import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;

import java.util.Optional;

/**
 * Options for EnMasse client
 */
public class ClientOptions {
    private final Source source;
    private final Target target;
    private final Optional<SslOptions> sslOptions;
    private final Optional<String> linkName;

    public ClientOptions(Source source, Target target, Optional<SslOptions> sslOptions, Optional<String> linkName) {
        this.source = source;
        this.target = target;
        this.sslOptions = sslOptions;
        this.linkName = linkName;
    }

    public Source getSource() {
        return source;
    }

    public Optional<SslOptions> getSslOptions() {
        return sslOptions;
    }

    public Target getTarget() {
        return target;
    }

    public Optional<String> getLinkName() {
        return linkName;
    }
}
