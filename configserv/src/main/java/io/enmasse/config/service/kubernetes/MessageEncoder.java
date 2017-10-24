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
