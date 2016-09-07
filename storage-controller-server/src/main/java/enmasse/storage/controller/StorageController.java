package enmasse.storage.controller;

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import enmasse.storage.controller.admin.ClusterManager;
import enmasse.storage.controller.admin.FlavorManager;
import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.openshift.OpenshiftClient;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lulf
 */
public class StorageController implements Runnable, AutoCloseable {
    private static final Logger log = Logger.getLogger(StorageController.class.getName());

    private final ProtonClient client;
    private final StorageControllerOptions options;
    private volatile ProtonConnection connection;

    private final ClusterManager clusterManager;
    private final FlavorManager flavorManager;
    private final Vertx vertx;

    public StorageController(StorageControllerOptions options) throws IOException {
        this.vertx = Vertx.vertx();
        client = ProtonClient.create(vertx);

        IClient osClient = new ClientBuilder(options.openshiftUrl())
                .usingToken(options.openshiftToken())
                .build();

        OpenshiftClient openshiftClient = new OpenshiftClient(osClient, options.openshiftNamespace());
        this.flavorManager = new FlavorManager();
        this.clusterManager = new ClusterManager(openshiftClient, new StorageGenerator(openshiftClient, flavorManager));
        this.options = options;
    }

    public void run() {
        client.connect(options.configHost(), options.configPort(), connectionHandle -> {
            if (connectionHandle.succeeded()) {
                connection = connectionHandle.result();
                connection.open();

                connection.createReceiver("flavor").handler(new ConfigAdapter(flavorManager::configUpdated))
                        .open();
                connection.createReceiver("maas").handler(new ConfigAdapter(clusterManager::configUpdated))
                        .open();
                log.log(Level.INFO, "Created receiver");
            } else {
                log.log(Level.INFO, "Connect failed: " + connectionHandle.cause().getMessage());
                vertx.close();
            }
        });
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}
