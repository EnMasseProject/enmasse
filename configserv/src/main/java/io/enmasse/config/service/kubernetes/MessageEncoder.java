/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.kubernetes;

import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.Set;

/**
 * Interface for encoding AMQP messages
 */
public interface MessageEncoder<T> {
    Message encode(Set<T> set) throws IOException;
}
