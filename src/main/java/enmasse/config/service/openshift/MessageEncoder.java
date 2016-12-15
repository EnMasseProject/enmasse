package enmasse.config.service.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.Set;

/**
 * Interface for encoding AMQP messages
 */
public interface MessageEncoder<T extends HasMetadata> {
    Message encode(Set<Resource<T>> set) throws IOException;
}
