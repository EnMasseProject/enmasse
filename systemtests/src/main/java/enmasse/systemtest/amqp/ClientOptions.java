package enmasse.systemtest.amqp;

import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;

import java.util.Optional;

/**
 * Options for EnMasse client
 */
public class ClientOptions {
    private final Source source;
    private final Target target;
    private final ProtonClientOptions protonClientOptions;
    private final Optional<String> linkName;

    public ClientOptions(Source source, Target target, ProtonClientOptions protonClientOptions, Optional<String> linkName) {
        this.source = source;
        this.target = target;
        this.protonClientOptions = protonClientOptions;
        this.linkName = linkName;
    }

    public Source getSource() {
        return (Source) source.copy();
    }

    public ProtonClientOptions getProtonClientOptions() {
        return protonClientOptions;
    }

    public Target getTarget() {
        return (Target) target.copy();
    }

    public Optional<String> getLinkName() {
        return linkName;
    }
}
