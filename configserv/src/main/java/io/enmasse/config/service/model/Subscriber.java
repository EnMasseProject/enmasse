package io.enmasse.config.service.model;

import org.apache.qpid.proton.message.Message;

/**
 * Represents an AMQP resource subscriber.
 */
public interface Subscriber {
    String getId();
    void resourcesUpdated(Message message);
}
