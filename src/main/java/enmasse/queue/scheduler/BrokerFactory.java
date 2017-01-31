package enmasse.queue.scheduler;

import io.vertx.proton.ProtonConnection;

import java.util.concurrent.Future;

/**
 * Factory for creating broker instances.
 */
public interface BrokerFactory {
    Future<Broker> createBroker(ProtonConnection connection);
}
