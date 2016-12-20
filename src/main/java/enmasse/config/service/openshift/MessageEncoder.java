package enmasse.config.service.openshift;

import enmasse.config.service.model.Resource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.Set;

/**
 * Interface for encoding AMQP messages
 */
public interface MessageEncoder<T extends Resource> {
    Message encode(Set<T> set) throws IOException;
}
