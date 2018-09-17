/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.authz;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        UserCredentials allowedUser = new UserCredentials("sender", "senderPa55");
        UserCredentials noAllowedUser = new UserCredentials("notallowedsender", "nobodyPa55");

        createUser(sharedAddressSpace, new User()
                .setUsername(allowedUser.getUsername())
                .setPassword(allowedUser.getPassword())
                .addAuthorization(new User.AuthorizationRule().addAddress("*").addOperation(User.Operation.SEND)));
        assertSend(allowedUser);
        removeUser(sharedAddressSpace, allowedUser.getUsername());

        createUser(sharedAddressSpace, new User()
                .setUsername(allowedUser.getUsername())
                .setPassword(allowedUser.getPassword())
                .addAuthorization(new User.AuthorizationRule()
                        .addOperation(User.Operation.SEND)
                        .addAddresses(addresses.stream().map(Destination::getAddress).collect(Collectors.toList()))));

        assertSend(allowedUser);
        removeUser(sharedAddressSpace, allowedUser.getUsername());

        createUser(sharedAddressSpace, new User()
                .setUsername(noAllowedUser.getUsername())
                .setPassword(noAllowedUser.getPassword()));
        assertCannotSend(noAllowedUser);
        removeUser(sharedAddressSpace, noAllowedUser.getUsername());

        createUser(sharedAddressSpace, new User()
                .setUsername(noAllowedUser.getUsername())
                .setPassword(noAllowedUser.getPassword())
                .addAuthorization(new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE)));
        assertCannotSend(noAllowedUser);
        removeUser(sharedAddressSpace, noAllowedUser.getUsername());
    }

    protected void doTestReceiveAuthz() throws Exception {
        initAddresses();
        UserCredentials allowedUser = new UserCredentials("receiver", "receiverPa55");
        UserCredentials noAllowedUser = new UserCredentials("notallowedreceiver", "nobodyPa55");

        createUser(sharedAddressSpace, new User()
                .setUsername(allowedUser.getUsername())
                .setPassword(allowedUser.getPassword())
                .addAuthorization(new User.AuthorizationRule().addAddress("*").addOperation(User.Operation.RECEIVE)));
        assertReceive(allowedUser);
        removeUser(sharedAddressSpace, allowedUser.getUsername());

        createUser(sharedAddressSpace, new User()
                .setUsername(allowedUser.getUsername())
                .setPassword(allowedUser.getPassword())
                .addAuthorization(new User.AuthorizationRule()
                        .addOperation(User.Operation.RECEIVE)
                        .addAddresses(addresses.stream().map(Destination::getAddress).collect(Collectors.toList()))));
        assertReceive(allowedUser);
        removeUser(sharedAddressSpace, allowedUser.getUsername());

        createUser(sharedAddressSpace, new User()
                .setUsername(noAllowedUser.getUsername())
                .setPassword(noAllowedUser.getPassword())
                .addAuthorization(new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.SEND)));
        assertCannotReceive(noAllowedUser);
        removeUser(sharedAddressSpace, noAllowedUser.getUsername());
    }

    protected void doTestUserPermissionAfterRemoveAuthz() throws Exception {
        initAddresses();
        UserCredentials user = new UserCredentials("pepa", "pepaPa55");

        createUser(sharedAddressSpace, new User()
                .setUsername(user.getUsername())
                .setPassword(user.getPassword())
                .addAuthorization(new User.AuthorizationRule()
                        .addOperation(User.Operation.RECEIVE)
                        .addAddress("*")));
        assertReceive(user);
        removeUser(sharedAddressSpace, user.getUsername());
        Thread.sleep(5000);

        createUser(sharedAddressSpace, new User()
                .setUsername(user.getUsername())
                .setPassword(user.getPassword())
                .addAuthorization(new User.AuthorizationRule()
                        .addOperation(User.Operation.RECEIVE)
                        .addAddress("pepa_address")));
        assertCannotReceive(user);
        removeUser(sharedAddressSpace, user.getUsername());
    }

    protected void doTestSendAuthzWithWIldcards() throws Exception {
        List<Destination> addresses = getAddressesWildcard();
        List<User> users = createUsersWildcard(sharedAddressSpace, User.Operation.SEND);

        setAddresses(addresses.toArray(new Destination[0]));

        for (User user : users) {
            for (Destination destination : addresses) {
                assertSendWildcard(user, destination);
            }
            removeUser(sharedAddressSpace, user.getUsername());
        }
    }

    protected void doTestReceiveAuthzWithWIldcards() throws Exception {
        List<Destination> addresses = getAddressesWildcard();
        List<User> users = createUsersWildcard(sharedAddressSpace, User.Operation.RECEIVE);

        setAddresses(addresses.toArray(new Destination[0]));

        for (User user : users) {
            for (Destination destination : addresses) {
                assertReceiveWildcard(user, destination);
            }
            removeUser(sharedAddressSpace, user.getUsername());
        }
    }

    //===========================================================================================================
    // Help methods
    //===========================================================================================================

    private void assertSendWildcard(User user, Destination destination) throws Exception {
        List<String> addresses = user.getAuthorization().stream()
                .map(authz -> authz.getAddresses().stream())
                .flatMap(Stream::distinct)
                .collect(Collectors.toList());

        UserCredentials credentials = new UserCredentials(user.getUsername(), user.getPassword());
        if (addresses.stream().filter(address -> destination.getAddress().contains(address.replace("*", ""))).collect(Collectors.toList()).size() > 0) {
            assertTrue(canSend(destination, credentials),
                    String.format("Authz failed, user %s cannot send message to destination %s", credentials,
                            destination.getAddress()));
        } else {
            assertFalse(canSend(destination, credentials),
                    String.format("Authz failed, user %s can send message to destination %s", credentials,
                            destination.getAddress()));
        }
    }

    private void assertReceiveWildcard(User user, Destination destination) throws Exception {
        List<String> addresses = user.getAuthorization().stream()
                .map(authz -> authz.getAddresses().stream())
                .flatMap(Stream::distinct)
                .collect(Collectors.toList());

        UserCredentials credentials = new UserCredentials(user.getUsername(), user.getPassword());
        if (addresses.stream().filter(address -> destination.getAddress().contains(address.replace("*", ""))).collect(Collectors.toList()).size() > 0) {
            assertTrue(canReceive(destination, credentials),
                    String.format("Authz failed, user %s cannot receive message from destination %s", credentials,
                            destination.getAddress()));
        } else {
            assertFalse(canReceive(destination, credentials),
                    String.format("Authz failed, user %s can receive message from destination %s", credentials,
                            destination.getAddress()));
        }
    }

    private void assertSend(UserCredentials credentials) throws Exception {
        log.info("Testing if client is authorized to send messages");
        String message = String.format("Authz failed, user %s cannot send message", credentials);
        assertTrue(canSend(queue, credentials), message);
        assertTrue(canSend(topic, credentials), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertTrue(canSend(multicast, credentials), message);
            assertTrue(canSend(anycast, credentials), message);
        }
    }

    private void assertCannotSend(UserCredentials credentials) throws Exception {
        log.info("Testing if client is NOT authorized to send messages");
        String message = String.format("Authz failed, user %s can send message", credentials);
        assertFalse(canSend(queue, credentials), message);
        assertFalse(canSend(topic, credentials), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertFalse(canSend(multicast, credentials), message);
            assertFalse(canSend(anycast, credentials), message);
        }
    }

    private void assertReceive(UserCredentials credentials) throws Exception {
        log.info("Testing if client is authorized to receive messages");
        String message = String.format("Authz failed, user %s cannot receive message", credentials);
        assertTrue(canReceive(queue, credentials), message);
        assertTrue(canReceive(topic, credentials), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertTrue(canReceive(multicast, credentials), message);
            assertTrue(canReceive(anycast, credentials), message);
        }
    }

    private void assertCannotReceive(UserCredentials credentials) throws Exception {
        log.info("Testing if client is NOT authorized to receive messages");
        String message = String.format("Authz failed, user %s can receive message", credentials);
        assertFalse(canReceive(queue, credentials), message);
        assertFalse(canReceive(topic, credentials), message);

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertFalse(canReceive(multicast, credentials), message);
            assertFalse(canReceive(anycast, credentials), message);
        }
    }

    private boolean canSend(Destination destination, UserCredentials credentials) throws Exception {
        logWithSeparator(log,
                String.format("Try send message under user %s from %s %s", credentials, destination.getType(), destination.getAddress()),
                String.format("***** Try to open sender client under user %s", credentials),
                String.format("***** Try to open receiver client under user %s", defaultCredentials));
        AmqpClient sender = createClient(destination, credentials);
        AmqpClient receiver = createClient(destination, defaultCredentials);
        logWithSeparator(log);
        return canAuth(sender, receiver, destination);
    }

    private boolean canReceive(Destination destination, UserCredentials credentials) throws Exception {
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
            Future<List<Message>> received = receiver.recvMessages(destination.getAddress(), 1, 2, TimeUnit.SECONDS);
            Future<Integer> sent = sender.sendMessages(destination.getAddress(), Collections.singletonList("msg1"), 2, TimeUnit.SECONDS);
            return received.get(3, TimeUnit.SECONDS).size() == sent.get(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.info("canAuth exception", ex);
            return false;
        } finally {
            sender.close();
            receiver.close();
        }
    }

    private AmqpClient createClient(Destination dest, UserCredentials credentials) throws Exception {
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

        Objects.requireNonNull(client).getConnectOptions().setCredentials(credentials);
        return client;
    }
}
