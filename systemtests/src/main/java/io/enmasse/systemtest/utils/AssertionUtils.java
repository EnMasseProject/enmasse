package io.enmasse.systemtest.utils;

import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.MessagingClientRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssertionUtils {

    public static void assertDefaultMessaging(MessagingClientRunner clientRunner) {
        int expectedMsgCount = 10;
        List<ExternalMessagingClient> clients = clientRunner.getClients();
        for (ExternalMessagingClient client : clients) {
            if (client.isSender()) {
                assertEquals(expectedMsgCount, client.getMessages().size(),
                        String.format("Expected %d sent messages", expectedMsgCount));
            } else {
                assertEquals(expectedMsgCount, client.getMessages().size(),
                        String.format("Expected %d received messages", expectedMsgCount));
            }
        }
    }
}
