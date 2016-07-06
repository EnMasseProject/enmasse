package enmasse.storage.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.authorization.TokenAuthorizationStrategy;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import enmasse.storage.controller.admin.ConfigManager;
import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.model.Config;
import enmasse.storage.controller.model.parser.ConfigParser;
import enmasse.storage.controller.openshift.OpenshiftClient;

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

    private final ConfigParser parser = new ConfigParser();
    private final ConfigManager configManager;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Vertx vertx;

    public StorageController(StorageControllerOptions options) throws IOException {
        this.vertx = Vertx.vertx();
        client = ProtonClient.create(vertx);

        IClient osClient = new ClientBuilder(options.openshiftUrl())
                .authorizationStrategy(new TokenAuthorizationStrategy(options.openshiftToken()))
                .build();

        OpenshiftClient openshiftClient = new OpenshiftClient(osClient, options.openshiftNamespace());
        configManager = new ConfigManager(openshiftClient, new StorageGenerator(openshiftClient, options.brokerProperties()));
        this.options = options;
    }

    public void run() {
        client.connect(options.configHost(), options.configPort(), connectionHandle -> {
            if (connectionHandle.succeeded()) {
                connection = connectionHandle.result();
                connection.open();

                connection.createReceiver("maas").handler(this::handleMessage).open();
                log.log(Level.INFO, "Created receiver");
            } else {
                log.log(Level.INFO, "Connect failed: " + connectionHandle.cause().getMessage());
                vertx.close();
            }
        });
    }

    private void handleMessage(ProtonDelivery protonDelivery, Message message) {
        try {
            if (message.getBody() instanceof AmqpValue) {
                JsonNode root = mapper.readTree((String) ((AmqpValue) message.getBody()).getValue());
                Config config = parser.parse(root.get("json"));
                log.log(Level.INFO, "Configuration was updated");

                configManager.configUpdated(config);
            }
        } catch (IOException e) {
            log.log(Level.INFO, "Error handling config update", e);
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}
