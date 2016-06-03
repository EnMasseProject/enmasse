package quilt.config.generator.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.restclient.ClientFactory;
import com.openshift.restclient.IClient;
import com.openshift.restclient.NoopSSLCertificateCallback;
import com.openshift.restclient.authorization.TokenAuthorizationStrategy;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import quilt.config.admin.ConfigManager;
import quilt.config.generator.ConfigGenerator;
import quilt.config.model.Config;
import quilt.config.model.parser.ConfigParser;
import quilt.config.openshift.OpenshiftClient;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lulf
 */
public class GeneratorAgent implements Runnable, AutoCloseable {
    private static final Logger log = Logger.getLogger(GeneratorAgent.class.getName());

    private final ProtonClient client;
    private final GeneratorAgentOptions options;
    private volatile ProtonConnection connection;

    private final ConfigParser parser = new ConfigParser();
    private final ConfigManager configManager;

    private final ObjectMapper mapper = new ObjectMapper();

    public GeneratorAgent(GeneratorAgentOptions options) throws IOException {
        client = ProtonClient.create(Vertx.vertx());

        IClient osClient = new ClientFactory().create(options.openshiftUrl(), new NoopSSLCertificateCallback());
        osClient.setAuthorizationStrategy(new TokenAuthorizationStrategy(options.openshiftToken()));

        configManager = new ConfigManager(new OpenshiftClient(osClient, options.openshiftNamespace()), new ConfigGenerator(osClient, options.brokerProperties()));
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
