/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.authz;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestBaseShared;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.vertx.proton.sasl.SaslSystemException;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;

import javax.security.sasl.AuthenticationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AuthorizationTestBase extends TestBase implements ITestBaseShared {

    private static Logger log = CustomLogger.getLogger();

    private Address queue;
    private Address topic;
    private Address anycast;
    private Address multicast;
    private List<Address> addresses;

    private void initAddresses() throws Exception {
        queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "authz-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("authz-queue")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "authz-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("authz-topic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();

        anycast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "authz-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("authz-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build();

        multicast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "authz-multicast"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("authz-multicast")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build();

        addresses = new ArrayList<>();
        addresses.add(queue);
        addresses.add(topic);
        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            addresses.add(anycast);
            addresses.add(multicast);
        }
        resourcesManager.setAddresses(addresses.toArray(new Address[0]));
    }

    protected void doTestSendAuthz() throws Exception {
        initAddresses();
        UserCredentials allowedUser = new UserCredentials("sender", "senderPa55");
        UserCredentials noAllowedUser = new UserCredentials("notallowedsender", "nobodyPa55");

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), UserUtils.createUserResource(allowedUser)
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder().withAddresses("*").withOperations(Operation.send).build()))
                .endSpec()
                .done());
        Thread.sleep(100);
        assertSend(allowedUser);
        resourcesManager.removeUser(getSharedAddressSpace(), allowedUser.getUsername());

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), UserUtils.createUserResource(allowedUser)
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withAddresses(addresses.stream().map(address -> address.getSpec().getAddress()).collect(Collectors.toList()))
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done());
        Thread.sleep(100);
        assertSend(allowedUser);
        resourcesManager.removeUser(getSharedAddressSpace(), allowedUser.getUsername());

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), UserUtils.createUserResource(noAllowedUser).done());
        assertCannotSend(noAllowedUser);
        resourcesManager.removeUser(getSharedAddressSpace(), noAllowedUser.getUsername());

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), UserUtils.createUserResource(noAllowedUser)
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.recv).build()))
                .endSpec()
                .done());
        assertCannotSend(noAllowedUser);
        resourcesManager.removeUser(getSharedAddressSpace(), noAllowedUser.getUsername());
    }

    protected void doTestReceiveAuthz() throws Exception {
        initAddresses();
        UserCredentials allowedUser = new UserCredentials("receiver", "receiverPa55");
        UserCredentials noAllowedUser = new UserCredentials("notallowedreceiver", "nobodyPa55");

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), UserUtils.createUserResource(allowedUser)
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder().withAddresses("*").withOperations(Operation.recv).build()))
                .endSpec()
                .done());
        assertReceive(allowedUser);
        resourcesManager.removeUser(getSharedAddressSpace(), allowedUser.getUsername());

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), UserUtils.createUserResource(allowedUser)
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withAddresses(addresses.stream().map(address -> address.getSpec().getAddress()).collect(Collectors.toList()))
                                .withOperations(Operation.recv).build()))
                .endSpec()
                .done());
        assertReceive(allowedUser);
        resourcesManager.removeUser(getSharedAddressSpace(), allowedUser.getUsername());

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), UserUtils.createUserResource(noAllowedUser)
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done());
        assertCannotReceive(noAllowedUser);
        resourcesManager.removeUser(getSharedAddressSpace(), noAllowedUser.getUsername());
    }

    protected void doTestUserPermissionAfterRemoveAuthz() throws Exception {
        initAddresses();
        UserCredentials user = new UserCredentials("pepa", "pepaPa55");

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), UserUtils.createUserResource(user)
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withOperations(Operation.recv)
                                .withAddresses("*").build()))
                .endSpec()
                .done());
        assertReceive(user);
        resourcesManager.removeUser(getSharedAddressSpace(), user.getUsername());
        Thread.sleep(5000);

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), UserUtils.createUserResource(user)
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withOperations(Operation.recv)
                                .withAddresses("pepa_address").build()))
                .endSpec()
                .done());
        assertCannotReceive(user);
        resourcesManager.removeUser(getSharedAddressSpace(), user.getUsername());
    }

    protected void doTestSendAuthzWithWIldcards() throws Exception {
        List<Address> addresses = getAddressesWildcard(getSharedAddressSpace());
        List<User> users = createUsersWildcard(getSharedAddressSpace(), Operation.send);

        resourcesManager.setAddresses(addresses.toArray(new Address[0]));

        for (User user : users) {
            for (Address destination : addresses) {
                assertSendWildcard(user, destination);
            }
            resourcesManager.removeUser(getSharedAddressSpace(), user.getSpec().getUsername());
        }
    }

    protected void doTestReceiveAuthzWithWIldcards() throws Exception {
        List<Address> addresses = getAddressesWildcard(getSharedAddressSpace());
        List<User> users = createUsersWildcard(getSharedAddressSpace(), Operation.recv);

        resourcesManager.setAddresses(addresses.toArray(new Address[0]));

        for (User user : users) {
            for (Address destination : addresses) {
                assertReceiveWildcard(user, destination);
            }
            resourcesManager.removeUser(getSharedAddressSpace(), user.getSpec().getUsername());
        }
    }

    //===========================================================================================================
    // Help methods
    //===========================================================================================================

    private void assertSendWildcard(User user, Address destination) throws Exception {
        List<String> addresses = user.getSpec().getAuthorization().stream()
                .map(authz -> authz.getAddresses().stream())
                .flatMap(Stream::distinct)
                .collect(Collectors.toList());

        UserCredentials credentials = UserUtils.getCredentialsFromUser(user);
        if (addresses.stream().filter(address -> destination.getSpec().getAddress().contains(address.replace("*", ""))).collect(Collectors.toList()).size() > 0) {
            assertTrue(canSend(destination, credentials),
                    String.format("Authz failed, user %s cannot send message to destination %s", credentials,
                            destination.getSpec().getAddress()));
        } else {
            assertFalse(canSend(destination, credentials),
                    String.format("Authz failed, user %s can send message to destination %s", credentials,
                            destination.getSpec().getAddress()));
        }
    }

    private void assertReceiveWildcard(User user, Address destination) throws Exception {
        List<String> addresses = user.getSpec().getAuthorization().stream()
                .map(authz -> authz.getAddresses().stream())
                .flatMap(Stream::distinct)
                .collect(Collectors.toList());

        UserCredentials credentials = UserUtils.getCredentialsFromUser(user);
        if (addresses.stream().filter(address -> destination.getSpec().getAddress().contains(address.replace("*", ""))).collect(Collectors.toList()).size() > 0) {
            assertTrue(canReceive(destination, credentials),
                    String.format("Authz failed, user %s cannot receive message from destination %s", credentials,
                            destination.getSpec().getAddress()));
        } else {
            assertFalse(canReceive(destination, credentials),
                    String.format("Authz failed, user %s can receive message from destination %s", credentials,
                            destination.getSpec().getAddress()));
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

    private boolean canSend(Address destination, UserCredentials credentials) throws Exception {
        logWithSeparator(log,
                String.format("Try send message under user %s from %s %s", credentials, destination.getSpec().getType(), destination.getSpec().getAddress()),
                String.format("***** Try to open sender client under user %s", credentials),
                String.format("***** Try to open receiver client under user %s", defaultCredentials));
        AmqpClient sender = createClient(destination, credentials);
        AmqpClient receiver = createClient(destination, defaultCredentials);
        logWithSeparator(log);
        return canAuth(sender, receiver, destination, true);
    }

    private boolean canReceive(Address destination, UserCredentials credentials) throws Exception {
        logWithSeparator(log,
                String.format("Try receive message under user %s from %s %s", credentials, destination.getSpec().getType(), destination.getSpec().getAddress()),
                String.format("***** Try to open sender client under user %s", defaultCredentials),
                String.format("***** Try to open receiver client under user %s", credentials));

        AmqpClient sender = createClient(destination, defaultCredentials);
        AmqpClient receiver = createClient(destination, credentials);
        logWithSeparator(log);
        return canAuth(sender, receiver, destination, false);
    }

    private boolean canAuth(AmqpClient sender, AmqpClient receiver, Address destination, boolean checkSender) throws Exception {
        try {
            Future<List<Message>> received = receiver.recvMessages(destination.getSpec().getAddress(), 1);
            Future<Integer> sent = sender.sendMessages(destination.getSpec().getAddress(), Collections.singletonList("msg1"));

            if (checkSender) {
                int numSent = sent.get(1, TimeUnit.MINUTES);
                log.info("Sent {}", numSent);
                int numReceived = received.get(1, TimeUnit.MINUTES).size();
                return numSent == numReceived;
            } else {
                int numReceived = received.get(1, TimeUnit.MINUTES).size();
                int numSent = sent.get(1, TimeUnit.MINUTES);
                return numSent == numReceived;
            }
        } catch (ExecutionException | SecurityException | UnauthorizedAccessException ex) {
            Throwable cause = ex;
            if (ex instanceof ExecutionException) {
                cause = ex.getCause();
            }

            if (cause instanceof SecurityException || cause instanceof SaslSystemException || cause instanceof AuthenticationException || cause instanceof UnauthorizedAccessException) {
                log.info("canAuth {} ({}): {}", destination.getSpec().getAddress(), destination.getSpec().getType(), ex.getMessage());
                return false;
            } else {
                log.warn("canAuth {} ({}) exception", destination.getSpec().getAddress(), destination.getSpec().getType(), ex);
                throw ex;
            }
        } finally {
            sender.close();
            receiver.close();
        }
    }

    private AmqpClient createClient(Address dest, UserCredentials credentials) throws Exception {
        AmqpClient client = null;

        switch (dest.getSpec().getType()) {
            case "queue":
            case "anycast":
                client = getAmqpClientFactory().createQueueClient(getSharedAddressSpace());
                break;
            case "topic":
                client = getAmqpClientFactory().createTopicClient(getSharedAddressSpace());
                break;
            case "multicast":
                client = getAmqpClientFactory().createBroadcastClient(getSharedAddressSpace());
                break;
        }

        Objects.requireNonNull(client).getConnectOptions().setCredentials(credentials);
        return client;
    }
}
