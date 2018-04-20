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

        createUser(sharedAddressSpace, allowedUser, Group.SEND_ALL_BROKERED.toString(), Group.SEND_ALL_STANDARD.toString());
        assertSend(allowedUser);
        removeUser(sharedAddressSpace, allowedUser.getUsername());

        createUser(sharedAddressSpace, allowedUser,
                addresses.stream().map(s -> "send_" + s.getAddress()).toArray(String[]::new));
        assertSend(allowedUser);
        removeUser(sharedAddressSpace, allowedUser.getUsername());

        createUser(sharedAddressSpace, noAllowedUser, "null");
        assertCannotSend(noAllowedUser);
        removeUser(sharedAddressSpace, noAllowedUser.getUsername());

        createUser(sharedAddressSpace, noAllowedUser, Group.RECV_ALL_BROKERED.toString(), Group.RECV_ALL_STANDARD.toString());
        assertCannotSend(noAllowedUser);
        removeUser(sharedAddressSpace, noAllowedUser.getUsername());
    }

    protected void doTestReceiveAuthz() throws Exception {
        initAddresses();
        KeycloakCredentials allowedUser = new KeycloakCredentials("receiver", "receiverPa55");
        KeycloakCredentials noAllowedUser = new KeycloakCredentials("notAllowedReceiver", "nobodyPa55");

        createUser(sharedAddressSpace, allowedUser, Group.RECV_ALL_BROKERED.toString(), Group.RECV_ALL_STANDARD.toString());
        assertReceive(allowedUser);
        removeUser(sharedAddressSpace, allowedUser.getUsername());

        createUser(sharedAddressSpace, allowedUser,
                addresses.stream().map(s -> "recv_" + s.getAddress()).toArray(String[]::new));
        assertReceive(allowedUser);
        removeUser(sharedAddressSpace, allowedUser.getUsername());

        createUser(sharedAddressSpace, noAllowedUser, Group.SEND_ALL_BROKERED.toString(), Group.SEND_ALL_STANDARD.toString());
        assertCannotReceive(noAllowedUser);
        removeUser(sharedAddressSpace, noAllowedUser.getUsername());
    }

    protected void doTestUserPermissionAfterRemoveAuthz() throws Exception {
        initAddresses();
        KeycloakCredentials user = new KeycloakCredentials("pepa", "pepaPa55");

        createUser(sharedAddressSpace, user, Group.RECV_ALL_BROKERED.toString(), Group.RECV_ALL_STANDARD.toString());
        assertReceive(user);
        removeUser(sharedAddressSpace, user.getUsername());

        createUser(sharedAddressSpace, user, "pepa_group");
        assertCannotReceive(user);
        removeUser(sharedAddressSpace, user.getUsername());
    }

    protected void doTestSendAuthzWithWIldcards() throws Exception {
        List<Destination> addresses = getAddressesWildcard();
        List<KeycloakCredentials> users = createUsersWildcard(sharedAddressSpace, "send");

        setAddresses(addresses.toArray(new Destination[0]));

        for (KeycloakCredentials user : users) {
            for (Destination destination : addresses) {
                assertSendWildcard(user, destination);
            }
        }
    }

    protected void doTestReceiveAuthzWithWIldcards() throws Exception {
        List<Destination> addresses = getAddressesWildcard();
        List<KeycloakCredentials> users = createUsersWildcard(sharedAddressSpace, "recv");

        setAddresses(addresses.toArray(new Destination[0]));

        for (KeycloakCredentials user : users) {
            for (Destination destination : addresses) {
                assertReceiveWildcard(user, destination);
            }
        }
    }

    //===========================================================================================================
    // Help methods
    //===========================================================================================================

    private void assertSendWildcard(KeycloakCredentials credentials, Destination destination) throws Exception {
        String rights = credentials.getUsername().replace("user_send_", "")
                .replace("*", "")
                .replace("#", "");
        if (rights.equals("") || destination.getAddress().contains(rights)) {
            assertTrue(canSend(destination, credentials),
                    String.format("Authz failed, user %s cannot send message to destination %s", credentials,
                            destination.getAddress()));
        } else {
            assertFalse(canSend(destination, credentials),
                    String.format("Authz failed, user %s can send message to destination %s", credentials,
                            destination.getAddress()));
        }
    }

    private void assertReceiveWildcard(KeycloakCredentials credentials, Destination destination) throws Exception {
        String rights = credentials.getUsername().replace("user_recv_", "")
                .replace("*", "")
                .replace("#", "");
        if (rights.equals("") || destination.getAddress().contains(rights)) {
            assertTrue(canReceive(destination, credentials),
                    String.format("Authz failed, user %s cannot receive message from destination %s", credentials,
                            destination.getAddress()));
        } else {
            assertFalse(canReceive(destination, credentials),
                    String.format("Authz failed, user %s can receive message from destination %s", credentials,
                            destination.getAddress()));
        }
    }

    private void assertSend(KeycloakCredentials credentials) throws Exception {
        log.info("Testing if client is authorized to send messages");
        String message = String.format("Authz failed, user %s cannot send message", credentials);
        assertTrue(canSend(queue, credentials), message);
        assertTrue(canSend(topic, credentials), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertTrue(canSend(multicast, credentials), message);
            assertTrue(canSend(anycast, credentials), message);
        }
    }

    private void assertCannotSend(KeycloakCredentials credentials) throws Exception {
        log.info("Testing if client is NOT authorized to send messages");
        String message = String.format("Authz failed, user %s can send message", credentials);
        assertFalse(canSend(queue, credentials), message);
        assertFalse(canSend(topic, credentials), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertFalse(canSend(multicast, credentials), message);
            assertFalse(canSend(anycast, credentials), message);
        }
    }

    private void assertReceive(KeycloakCredentials credentials) throws Exception {
        log.info("Testing if client is authorized to receive messages");
        String message = String.format("Authz failed, user %s cannot receive message", credentials);
        assertTrue(canReceive(queue, credentials), message);
        assertTrue(canReceive(topic, credentials), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertTrue(canReceive(multicast, credentials), message);
            assertTrue(canReceive(anycast, credentials), message);
        }
    }

    private void assertCannotReceive(KeycloakCredentials credentials) throws Exception {
        log.info("Testing if client is NOT authorized to receive messages");
        String message = String.format("Authz failed, user %s can receive message", credentials);
        assertFalse(canReceive(queue, credentials), message);
        assertFalse(canReceive(topic, credentials), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertFalse(canReceive(multicast, credentials), message);
            assertFalse(canReceive(anycast, credentials), message);
        }
    }

    private boolean canSend(Destination destination, KeycloakCredentials credentials) throws Exception {
        logWithSeparator(log,
                String.format("Try send message under user %s from %s %s", credentials, destination.getType(), destination.getAddress()),
                String.format("***** Try to open sender client under user %s", credentials),
                String.format("***** Try to open receiver client under user %s", defaultCredentials));
        AmqpClient sender = createClient(destination, credentials);
        AmqpClient receiver = createClient(destination, defaultCredentials);
        logWithSeparator(log);
        return canAuth(sender, receiver, destination);
    }

    private boolean canReceive(Destination destination, KeycloakCredentials credentials) throws Exception {
        logWithSeparator(log,
                String.format("Try receive message under user %s from %s %s", credentials, destination.getType(), destination.getAddress()),
                String.format("***** Try to open sender client under user %s", defaultCredentials),
                String.format("***** Try to open receiver client under user %s", credentials));

        AmqpClient sender = createClient(destination, defaultCredentials);
        AmqpClient receiver = createClient(destination, credentials);
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

    private AmqpClient createClient(Destination dest, KeycloakCredentials credentials) throws Exception {
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

        client.getConnectOptions().setCredentials(credentials);
        return client;
    }
}
