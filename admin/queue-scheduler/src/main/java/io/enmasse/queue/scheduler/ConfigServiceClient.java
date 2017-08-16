package io.enmasse.queue.scheduler;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.types.standard.StandardType;
import io.enmasse.address.model.v1.CodecV1;
import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
                    vertx.setTimer(10000, id -> connectToConfigService(client));
                });
                configConnection.open();

                ProtonReceiver receiver = configConnection.createReceiver("v1/addresses");
                receiver.closeHandler(result -> {
                    configConnection.close();
                    vertx.setTimer(10000, id -> connectToConfigService(client));
                });
                receiver.handler((protonDelivery, message) -> {
                    String payload = (String)((AmqpValue)message.getBody()).getValue();
                    Map<String, Set<Address>> addressConfig = decodeAddressConfig(payload);

                    configListener.addressesChanged(addressConfig);
                });
                receiver.open();
            } else {
                log.error("Error connecting to configuration service", connResult.cause());
                vertx.setTimer(10000, id -> connectToConfigService(client));
            }
        });
    }

    private Map<String, Set<Address>> decodeAddressConfig(String payload) {
        try {

            AddressList addressList = CodecV1.getMapper().readValue(payload, AddressList.class);

            Map<String, Set<Address>> addressMap = new LinkedHashMap<>();
            for (Address address : addressList) {
                if (isQueue(address)) {
                    String clusterId = getClusterIdForQueue(address);
                    Set<Address> addresses = addressMap.computeIfAbsent(clusterId, k -> new HashSet<>());
                    addresses.add(address);
                }
            }
            return addressMap;
        } catch (IOException e) {
            throw new RuntimeException("Error decoding JSON payload '"+payload+"'", e);
        }
    }

    private boolean isQueue(Address address) {
        return StandardType.QUEUE.equals(address.getType());
    }


    // TODO: Put this constant somewhere appropriate
    private boolean isPooled(Address address) {
        return address.getPlan().getName().startsWith("pooled");
    }

    //TODO: This logic is replicated from AddressController (and is also horrid and broken)
    private String getClusterIdForQueue(Address address) {
        if (isPooled(address)) {
            return address.getPlan().getName();
        } else {
            return address.getName();
        }
    }

    @Override
    public void stop() {
        if (configConnection != null) {
            configConnection.close();
        }
    }
}
