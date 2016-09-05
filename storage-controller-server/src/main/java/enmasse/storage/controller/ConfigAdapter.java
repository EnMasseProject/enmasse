package enmasse.storage.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.storage.controller.admin.ConfigSubscriber;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonMessageHandler;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ulf Lilleengen
 */
public class ConfigAdapter implements ProtonMessageHandler {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = Logger.getLogger(ConfigAdapter.class.getName());

    private final ConfigSubscriber configSubscriber;

    public ConfigAdapter(ConfigSubscriber configSubscriber) {
        this.configSubscriber = configSubscriber;
    }

    @Override
    public void handle(ProtonDelivery delivery, Message message) {
        try {
            if (message.getBody() instanceof AmqpValue) {
                JsonNode root = mapper.readTree((String) ((AmqpValue) message.getBody()).getValue());
                configSubscriber.configUpdated(root.get("json"));
            }
        } catch (IOException e) {
            log.log(Level.INFO, "Error handling address config update", e);
        }

    }
}
