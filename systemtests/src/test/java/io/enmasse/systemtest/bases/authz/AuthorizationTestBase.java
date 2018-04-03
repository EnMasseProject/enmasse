/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.authz;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AuthorizationTestBase extends TestBaseWithShared {

    private static Logger log = CustomLogger.getLogger();
    private final Destination queue = Destination.queue("authz-queue", getDefaultPlan(AddressType.QUEUE));
    private final Destination topic = Destination.topic("authz-topic", getDefaultPlan(AddressType.TOPIC));
    private final Destination anycast = Destination.anycast("authz-anycast");
    private final Destination multicast = Destination.multicast("authz-multicast");
    private List<Destination> addresses;

    private void initAddresses() throws Exception {
        addresses = new ArrayList<>();
        addresses.add(queue);
        addresses.add(topic);
        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            addresses.add(anycast);
            addresses.add(multicast);
        }
        setAddresses(addresses.toArray(new Destination[0]));
    }

    protected void doTestSendAuthz() throws Exception {
        initAddresses();
        KeycloakCredentials allowedUser = new KeycloakCredentials("sender", "senderPa55");
        KeycloakCredentials noAllowedUser = new KeycloakCredentials("notAllowedSender", "nobodyPa55");

        getKeycloakClient().createUser(sharedAddressSpace.getName(), allowedUser.getUsername(), allowedUser.getPassword(), Group.SEND_ALL_BROKERED.toString(), Group.SEND_ALL_STANDARD.toString());
        assertSend(allowedUser.getUsername(), allowedUser.getPassword());
        getKeycloakClient().deleteUser(sharedAddressSpace.getName(), allowedUser.getUsername());

        getKeycloakClient().createUser(sharedAddressSpace.getName(), allowedUser.getUsername(), allowedUser.getPassword(),
                addresses.stream().map(s -> "send_" + s.getAddress()).toArray(String[]::new));
        assertSend(allowedUser.getUsername(), allowedUser.getPassword());
        getKeycloakClient().deleteUser(sharedAddressSpace.getName(), allowedUser.getUsername());

        getKeycloakClient().createUser(sharedAddressSpace.getName(), noAllowedUser.getUsername(), noAllowedUser.getPassword(), "null");
        assertCannotSend(noAllowedUser.getUsername(), noAllowedUser.getPassword());
        getKeycloakClient().deleteUser(sharedAddressSpace.getName(), noAllowedUser.getUsername());

        getKeycloakClient().createUser(sharedAddressSpace.getName(), noAllowedUser.getUsername(), noAllowedUser.getPassword(), Group.RECV_ALL_BROKERED.toString(), Group.RECV_ALL_STANDARD.toString());
        assertCannotSend(noAllowedUser.getUsername(), noAllowedUser.getPassword());
        getKeycloakClient().deleteUser(sharedAddressSpace.getName(), noAllowedUser.getUsername());
    }

    protected void doTestReceiveAuthz() throws Exception {
        initAddresses();
        KeycloakCredentials allowedUser = new KeycloakCredentials("receiver", "receiverPa55");
        KeycloakCredentials noAllowedUser = new KeycloakCredentials("notAllowedReceiver", "nobodyPa55");

        getKeycloakClient().createUser(sharedAddressSpace.getName(), allowedUser.getUsername(), allowedUser.getPassword(), Group.RECV_ALL_BROKERED.toString(), Group.RECV_ALL_STANDARD.toString());
        assertReceive(allowedUser.getUsername(), allowedUser.getPassword());
        getKeycloakClient().deleteUser(sharedAddressSpace.getName(), allowedUser.getUsername());

        getKeycloakClient().createUser(sharedAddressSpace.getName(), allowedUser.getUsername(), allowedUser.getPassword(),
                addresses.stream().map(s -> "recv_" + s.getAddress()).toArray(String[]::new));
        assertReceive(allowedUser.getUsername(), allowedUser.getPassword());
        getKeycloakClient().deleteUser(sharedAddressSpace.getName(), allowedUser.getUsername());

        getKeycloakClient().createUser(sharedAddressSpace.getName(), noAllowedUser.getUsername(), noAllowedUser.getPassword(), Group.SEND_ALL_BROKERED.toString(), Group.SEND_ALL_STANDARD.toString());
        assertCannotReceive(noAllowedUser.getUsername(), noAllowedUser.getPassword());
        getKeycloakClient().deleteUser(sharedAddressSpace.getName(), noAllowedUser.getUsername());
    }

    protected void doTestUserPermissionAfterRemoveAuthz() throws Exception {
        initAddresses();
        KeycloakCredentials user = new KeycloakCredentials("pepa", "pepaPa55");

        getKeycloakClient().createUser(sharedAddressSpace.getName(), user.getUsername(), user.getPassword(), Group.RECV_ALL_BROKERED.toString(), Group.RECV_ALL_STANDARD.toString());
        assertReceive(user.getUsername(), user.getPassword());
        getKeycloakClient().deleteUser(sharedAddressSpace.getName(), user.getUsername());

        getKeycloakClient().createUser(sharedAddressSpace.getName(), user.getUsername(), user.getPassword(), "pepa_group");
        assertCannotReceive(user.getUsername(), user.getPassword());
        getKeycloakClient().deleteUser(sharedAddressSpace.getName(), user.getUsername());
    }

    protected void doTestSendAuthzWithWIldcards() throws Exception {
        List<Destination> addresses = getAddressesWildcard();
        List<KeycloakCredentials> users = createUsersWildcard(sharedAddressSpace, "send");

        setAddresses(addresses.toArray(new Destination[0]));

        for (KeycloakCredentials user : users) {
            for (Destination destination : addresses) {
                assertSendWildcard(user.getUsername(), user.getPassword(), destination);
            }
        }
    }

    protected void doTestReceiveAuthzWithWIldcards() throws Exception {
        List<Destination> addresses = getAddressesWildcard();
        List<KeycloakCredentials> users = createUsersWildcard(sharedAddressSpace, "recv");

        setAddresses(addresses.toArray(new Destination[0]));

        for (KeycloakCredentials user : users) {
            for (Destination destination : addresses) {
                assertReceiveWildcard(user.getUsername(), user.getPassword(), destination);
            }
        }
    }

    //===========================================================================================================
    // Help methods
    //===========================================================================================================

    private void assertSendWildcard(String username, String password, Destination destination) throws Exception {
        String rights = username.replace("user_send_", "")
                .replace("*", "")
                .replace("#", "");
        if (rights.equals("") || destination.getAddress().contains(rights)) {
            assertTrue(canSend(destination, username, password),
                    String.format("Authz failed, user %s cannot send message to destination %s", username,
                                destination.getAddress()));
        } else {
            assertFalse(canSend(destination, username, password),
                    String.format("Authz failed, user %s can send message to destination %s", username,
                                destination.getAddress()));
        }
    }

    private void assertReceiveWildcard(String username, String password, Destination destination) throws Exception {
        String rights = username.replace("user_recv_", "")
                .replace("*", "")
                .replace("#", "");
        if (rights.equals("") || destination.getAddress().contains(rights)) {
            assertTrue(canReceive(destination, username, password),
                    String.format("Authz failed, user %s cannot receive message from destination %s", username,
                                destination.getAddress()));
        } else {
            assertFalse(canReceive(destination, username, password),
                    String.format("Authz failed, user %s can receive message from destination %s", username,
                                destination.getAddress()));
        }
    }

    private void assertSend(String username, String password) throws Exception {
        log.info("Testing if client is authorized to send messages");
        String message = String.format("Authz failed, user %s cannot send message", username);
        assertTrue(canSend(queue, username, password), message);
        assertTrue(canSend(topic, username, password), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertTrue(canSend(multicast, username, password), message);
            assertTrue(canSend(anycast, username, password), message);
        }
    }

    private void assertCannotSend(String username, String password) throws Exception {
        log.info("Testing if client is NOT authorized to send messages");
        String message = String.format("Authz failed, user %s can send message", username);
        assertFalse(canSend(queue, username, password), message);
        assertFalse(canSend(topic, username, password), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertFalse(canSend(multicast, username, password), message);
            assertFalse(canSend(anycast, username, password), message);
        }
    }

    private void assertReceive(String username, String password) throws Exception {
        log.info("Testing if client is authorized to receive messages");
        String message = String.format("Authz failed, user %s cannot receive message", username);
        assertTrue(canReceive(queue, username, password), message);
        assertTrue(canReceive(topic, username, password), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertTrue(canReceive(multicast, username, password), message);
            assertTrue(canReceive(anycast, username, password), message);
        }
    }

    private void assertCannotReceive(String username, String password) throws Exception {
        log.info("Testing if client is NOT authorized to receive messages");
        String message = String.format("Authz failed, user %s can receive message", username);
        assertFalse(canReceive(queue, username, password), message);
        assertFalse(canReceive(topic, username, password), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertFalse(canReceive(multicast, username, password), message);
            assertFalse(canReceive(anycast, username, password), message);
        }
    }

    private boolean canSend(Destination destination, String username, String password) throws Exception {
        logWithSeparator(log,
                String.format("Try send message under user %s from %s %s", username, destination.getType(), destination.getAddress()),
                String.format("***** Try to open sender client under user %s", username),
                String.format("***** Try to open receiver client under user %s", this.username));
        AmqpClient sender = createClient(destination, username, password);
        AmqpClient receiver = createClient(destination, this.username, this.password);
        logWithSeparator(log);
        return canAuth(sender, receiver, destination);
    }

    private boolean canReceive(Destination destination, String username, String password) throws Exception {
        logWithSeparator(log,
                String.format("Try receive message under user %s from %s %s", username, destination.getType(), destination.getAddress()),
                String.format("***** Try to open sender client under user %s", this.username),
                String.format("***** Try to open receiver client under user %s", username));

        AmqpClient sender = createClient(destination, this.username, this.password);
        AmqpClient receiver = createClient(destination, username, password);
        logWithSeparator(log);
        return canAuth(sender, receiver, destination);
    }

    private boolean canAuth(AmqpClient sender, AmqpClient receiver, Destination destination) throws Exception {
        try {
            Future<List<Message>> received = receiver.recvMessages(destination.getAddress(), 1, 10, TimeUnit.SECONDS);
            Future<Integer> sent = sender.sendMessages(destination.getAddress(), Collections.singletonList("msg1"), 10, TimeUnit.SECONDS);
            return received.get(10, TimeUnit.SECONDS).size() == sent.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            sender.close();
            receiver.close();
            return false;
        }
    }

    private AmqpClient createClient(Destination dest, String username, String password) throws Exception {
        AmqpClient client = null;

        switch (dest.getType()) {
            case "queue":
                client = amqpClientFactory.createQueueClient(sharedAddressSpace);
                break;
            case "topic":
                client = amqpClientFactory.createTopicClient(sharedAddressSpace);
                break;
            case "anycast":
                client = amqpClientFactory.createQueueClient(sharedAddressSpace);
                break;
            case "multicast":
                client = amqpClientFactory.createBroadcastClient(sharedAddressSpace);
                break;
        }

        client.getConnectOptions().setUsername(username).setPassword(password);
        return client;
    }
}
