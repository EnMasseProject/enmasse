package enmasse.queue.scheduler;

import io.vertx.proton.ProtonConnection;

/**
 * TODO: Description
 */
public class Artemis implements Broker {

    private final ProtonConnection connection;
    private final String id;

    public Artemis(ProtonConnection connection) {
        this.connection = connection;
        this.id = connection.getRemoteContainer();
        createManagementLink();
    }

    private void createManagementLink() {
        connection.createSender("$management");
    }

    @Override
    public void deployQueue(String address) {

    }

    @Override
    public void deleteQueue(String address) {

    }

    @Override
    public long numQueues() {
        return 0;
    }

    @Override
    public String getId() {
        return null;
    }
}
