package io.enmasse.systemtest.messagingclients;

import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointPort;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessagingClientRunner {
    Logger LOGGER;

    public MessagingClientRunner() {
        LOGGER = CustomLogger.getLogger();
    }

    public List<ExternalMessagingClient> sendAndReceive(MessagingEndpoint endpoint, boolean waitReceivers, String senderAddress, String ... receiverAddresses) throws Exception {
        return sendAndReceive(endpoint, waitReceivers, null, null, senderAddress, receiverAddresses);
    }

    public List<ExternalMessagingClient> sendAndReceive(MessagingEndpoint endpoint, String senderAddress, String ... receiverAddresses) throws Exception {
        return sendAndReceive(endpoint, false, null, null, senderAddress, receiverAddresses);
    }

    /**
     * Send 10 messages on sender address, and receive 10 messages on each receiver address.
     */
    public List<ExternalMessagingClient> sendAndReceive(MessagingEndpoint endpoint, boolean waitReceivers, Map<ClientArgument, Object> extraSenderArgs,
                           Map<ClientArgument, Object> extraReceiverArgs, String senderAddress, String ... receiverAddresses) throws Exception {
        int expectedMsgCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(1 + receiverAddresses.length);
        List<ExternalMessagingClient> clients = new ArrayList<>();
        Endpoint e = new Endpoint(endpoint.getStatus().getHost(), getPort("AMQP", endpoint));
        ExternalMessagingClient senderClient = new ExternalMessagingClient(false)
                .withClientEngine(new RheaClientSender())
                .withMessagingRoute(e)
                .withAddress(senderAddress)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                .withTimeout(60);

        if (extraSenderArgs != null) {
            for (Map.Entry<ClientArgument, Object> arg : extraSenderArgs.entrySet()) {
                senderClient.withAdditionalArgument(arg.getKey(), arg.getValue());
            }
        }

        for (String receiverAddress : receiverAddresses) {
            ExternalMessagingClient receiverClient = new ExternalMessagingClient(false)
                    .withClientEngine(new RheaClientReceiver())
                    .withMessagingRoute(e)
                    .withAddress(receiverAddress)
                    .withCount(expectedMsgCount)
                    .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                    .withTimeout(60);

            if (extraReceiverArgs != null) {
                for (Map.Entry<ClientArgument, Object> arg : extraReceiverArgs.entrySet()) {
                    receiverClient.withAdditionalArgument(arg.getKey(), arg.getValue());
                }
            }

            clients.add(receiverClient);
        }

        List<Future<Boolean>> receiverResults = new ArrayList<>();
        for (ExternalMessagingClient receiverClient : clients) {
            receiverResults.add(executor.submit((Callable<Boolean>) receiverClient::run));
        }

        if (waitReceivers) {
            // To ensure receivers are attached and ready
            Thread.sleep(10_000);
        }

        Future<Boolean> senderResult = executor.submit((Callable<Boolean>) senderClient::run);
        clients.add(senderClient);
        assertTrue(senderResult.get(1, TimeUnit.MINUTES), "Sender failed, expected return code 0");
        for (Future<Boolean> receiverResult : receiverResults) {
            assertTrue(receiverResult.get(1, TimeUnit.MINUTES), "Receiver failed, expected return code 0");
        }
        return clients;
    }


    public int getPort(String protocol, MessagingEndpoint endpoint) {
        for (MessagingEndpointPort port : endpoint.getStatus().getPorts()) {
            if (protocol.equals(port.getProtocol()))  {
                return port.getPort();
            }
        }
        return 0;
    }
}
