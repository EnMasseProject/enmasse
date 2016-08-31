package enmasse.broker.forwarder;

import org.apache.qpid.proton.message.Message;

/**
 * @author Ulf Lilleengen
 */
public interface ReplicatedBroker {
    void forwardMessage(Message message, String forwardAddress);
    void close();
}
