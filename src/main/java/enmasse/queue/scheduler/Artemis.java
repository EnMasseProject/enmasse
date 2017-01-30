package enmasse.queue.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Represents an Artemis broker that may be managed
 */
public class Artemis implements Broker {
    private static final Logger log = LoggerFactory.getLogger(Artemis.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Vertx vertx;
    private final ProtonSender sender;
    private final String replyTo;
    private final BlockingQueue<Message> replies;

    public Artemis(Vertx vertx, ProtonSender sender, String replyTo, BlockingQueue<Message> replies) {
        this.vertx = vertx;
        this.sender = sender;
        this.replyTo = replyTo;
        this.replies = replies;
    }

    public static Future<Artemis> create(Vertx vertx, ProtonConnection connection) {
        CompletableFuture<Artemis> promise = new CompletableFuture<>();
        connection.sessionOpenHandler(ProtonSession::open);
        BlockingQueue<Message> replies = new LinkedBlockingDeque<>();
        ProtonSender sender = connection.createSender("activemq.management");
        sender.openHandler(result -> {
            System.out.println("Opened sender");
            ProtonReceiver receiver = connection.createReceiver("activemq.management");
            Source source = new Source();
            source.setDynamic(true);
            receiver.setSource(source);
            receiver.openHandler(h -> {
                promise.complete(new Artemis(vertx, sender, h.result().getRemoteSource().getAddress(), replies));
            });
            receiver.handler(((protonDelivery, message) -> {
                System.out.println("Got new message!");
                try {
                    replies.put(message);
                    ProtonHelper.accepted(protonDelivery, true);
                } catch (Exception e) {
                    ProtonHelper.rejected(protonDelivery, true);
                }
            }));
            receiver.open();
        });
        sender.open();
        return promise;
    }

    @Override
    public void deployQueue(String address) {
        Message message = createMessage("deployQueue");
        ArrayNode parameters = mapper.createArrayNode();
        parameters.add(address);
        parameters.add(address);
        parameters.addNull();
        parameters.add(false);

        message.setBody(new AmqpValue(encodeJson(parameters)));
        doRequest(message);
    }

    private Message doRequest(Message message) {
        vertx.runOnContext(h -> sender.send(message));
        try {
            return replies.poll(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String encodeJson(ArrayNode parameters) {
        try {
            return mapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public void deleteQueue(String address) {
        Message message = createMessage("destroyQueue");
        ArrayNode parameters = mapper.createArrayNode();
        parameters.add(address);
        parameters.add(true);
        message.setBody(new AmqpValue(encodeJson(parameters)));

        doRequest(message);
    }

    private Message createMessage(String operation) {
        Message message = Message.Factory.create();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("_AMQ_ResourceName", "broker");
        properties.put("_AMQ_OperationName", operation);
        properties.put("JMSReplyTo", replyTo);
        message.setApplicationProperties(new ApplicationProperties(properties));
        return message;
    }

    @Override
    public long numQueues() {
        Message message = createMessage("getQueueNames");
        message.setBody(new AmqpValue("[]"));

        // TODO: Make this method less ugly
        Message response = doRequest(message);
        if (response == null) {
            log.warn("Timed out getting response from broker");
            return -1;
        }
        AmqpValue value = (AmqpValue) response.getBody();
        long numQueues = 0;
        try {
            ArrayNode root = (ArrayNode) mapper.readTree((String) value.getValue());
            ArrayNode elements = (ArrayNode) root.get(0);
            for (int i = 0; i < elements.size(); i++) {
                String queueName = elements.get(i).asText();
                if (!queueName.equals(replyTo)) {
                    numQueues++;
                }
            }
        } catch (IOException e) {
            log.error("Error decoding queue names", e);
            return -1;
        }
        return numQueues;
    }
}
