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

    public ClientOptions(Source source, Target target, Optional<SslOptions> sslOptions) {
        this.source = source;
        this.target = target;
        this.sslOptions = sslOptions;
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
}
