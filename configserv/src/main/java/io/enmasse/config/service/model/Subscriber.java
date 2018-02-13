/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.model;

import org.apache.qpid.proton.message.Message;

/**
 * Represents an AMQP resource subscriber.
 */
public interface Subscriber {
    String getId();
    void resourcesUpdated(Message message);
}
