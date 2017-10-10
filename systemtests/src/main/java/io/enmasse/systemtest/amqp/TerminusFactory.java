package io.enmasse.systemtest.amqp;

import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;

public interface TerminusFactory {
    Source getSource(String address);
    Target getTarget(String address);
}
