package enmasse.queue.scheduler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Client connecting to the configuration service.
 */
public class ConfigServiceClient extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ConfigServiceClient.class.getName());
    private final String configHost;
    private final int configPort;
    private final ConfigListener configListener;
    private volatile ProtonConnection configConnection;

    public ConfigServiceClient(String configHost, int configPort, ConfigListener configListener) {
        this.configHost = configHost;
        this.configPort = configPort;
        this.configListener = configListener;
    }

    @Override
    public void start() {
        connectToConfigService(ProtonClient.create(vertx));
    }

    private void connectToConfigService(ProtonClient client) {
        client.connect(configHost, configPort, connResult -> {
            if (connResult.succeeded()) {
                log.info("Connected to the configuration service");
                configConnection = connResult.result();
                configConnection.closeHandler(result -> {
                    vertx.setTimer(5000, id -> connectToConfigService(client));
                });
                configConnection.open();

                ProtonReceiver receiver = configConnection.createReceiver("maas");
                receiver.closeHandler(result -> {
                    configConnection.close();
                    vertx.setTimer(5000, id -> connectToConfigService(client));
                });
                receiver.handler((protonDelivery, message) -> {
                    String payload = (String)((AmqpValue)message.getBody()).getValue();
                    Map<String, Set<String>> addressConfig = decodeAddressConfig(new JsonObject(payload));
                    configListener.addressesChanged(addressConfig);
                });
                receiver.open();
            } else {
                log.error("Error connecting to configuration service", connResult.cause());
                vertx.setTimer(5000, id -> connectToConfigService(client));
            }
        });
    }

    private Map<String, Set<String>> decodeAddressConfig(JsonObject payload) {
        Map<String, Set<String>> addressMap = new LinkedHashMap<>();
        for (String address : payload.fieldNames()) {
            JsonObject addressObject = payload.getJsonObject(address);
            String groupId = addressObject.getString("group_id");
            Set<String> addresses = addressMap.get(groupId);
            if (addresses == null) {
                addresses = new HashSet<>();
                addressMap.put(groupId, addresses);
            }
            addresses.add(address);
        }
        return addressMap;
    }

    @Override
    public void stop() {
        if (configConnection != null) {
            configConnection.close();
        }
    }
}
