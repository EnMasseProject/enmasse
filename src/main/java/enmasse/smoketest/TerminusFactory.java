package enmasse.smoketest;

import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;

public interface TerminusFactory {
    Source getSource(String address);
    Target getTarget(String address);
}
