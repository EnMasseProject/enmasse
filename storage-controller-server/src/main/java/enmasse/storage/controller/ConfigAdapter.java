package enmasse.storage.controller;

import com.fasterxml.jackson.databind.JsonNode;
import enmasse.storage.controller.admin.ConfigManager;
import enmasse.storage.controller.admin.FlavorManager;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonMessageHandler;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.vertx.core.json.Json.mapper;

/**
 * @author Ulf Lilleengen
 */
public class ConfigAdapter implements ProtonMessageHandler {
    private static final Logger log = Logger.getLogger(ConfigAdapter.class.getName());

    private final ConfigManager configManager;

    public ConfigAdapter(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void handle(ProtonDelivery delivery, Message message) {
        try {
            if (message.getBody() instanceof AmqpValue) {
                JsonNode root = mapper.readTree((String) ((AmqpValue) message.getBody()).getValue());
                configManager.configUpdated(root.get("json"));
            }
        } catch (IOException e) {
            log.log(Level.INFO, "Error handling address config update", e);
        }

    }
}
