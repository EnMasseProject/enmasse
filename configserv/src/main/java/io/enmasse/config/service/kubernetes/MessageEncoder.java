package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.model.Resource;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.Set;

/**
 * Interface for encoding AMQP messages
 */
public interface MessageEncoder<T extends Resource> {
    Message encode(Set<T> set) throws IOException;
}
