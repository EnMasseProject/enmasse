package enmasse.queue.scheduler;

/**
 * Represents a broker that may be assigned multiple addresses
 */
public interface Broker {
    void deployQueue(String address);
    void deleteQueue(String address);
    long numQueues();
}
