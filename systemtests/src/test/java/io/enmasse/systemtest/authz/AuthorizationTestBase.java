package io.enmasse.systemtest.authz;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import org.apache.qpid.proton.message.Message;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AuthorizationTestBase extends TestBaseWithDefault {

    private static final Destination queue = Destination.queue("authz-queue");
    private static final Destination topic = Destination.topic("authz-topic");
    private static final Destination anycast = Destination.anycast("authz-anycast");
    private static final Destination multicast = Destination.multicast("authz-multicast");

    @Before
    public void initAddresses() throws Exception {
        List<Destination> addresses = new ArrayList<>();
        addresses.add(queue);
        addresses.add(topic);
        if(getAddressSpaceType() == AddressSpaceType.STANDARD){
            addresses.add(anycast);
            addresses.add(multicast);
        }
        setAddresses(defaultAddressSpace, addresses.toArray(new Destination[0]));
    }

    protected void doTestSendAuthz() throws Exception {
        KeycloakCredentials allowedUser = new KeycloakCredentials("sender", "senderPa55");
        getKeycloakClient().createUser(defaultAddressSpace.getName(), allowedUser.getUsername(), allowedUser.getPassword(), Group.SEND_ALL.toString());
        assertSend(allowedUser.getUsername(), allowedUser.getPassword());
        getKeycloakClient().deleteUser(defaultAddressSpace.getName(), allowedUser.getUsername());

        KeycloakCredentials noAllowedUser = new KeycloakCredentials("nobody", "nobodyPa55");
        getKeycloakClient().createUser(defaultAddressSpace.getName(), noAllowedUser.getUsername(), noAllowedUser.getPassword(), Group.RECV_ALL.toString());
        assertCannotSend(noAllowedUser.getUsername(), noAllowedUser.getPassword());
        getKeycloakClient().deleteUser(defaultAddressSpace.getName(), noAllowedUser.getUsername());

        getKeycloakClient().createUser(defaultAddressSpace.getName(), noAllowedUser.getUsername(), noAllowedUser.getPassword(), "null");
        assertCannotSend(noAllowedUser.getUsername(), noAllowedUser.getPassword());
        getKeycloakClient().deleteUser(defaultAddressSpace.getName(), noAllowedUser.getUsername());
    }

    protected void doTestSendReceiveAuthz() throws Exception {
        KeycloakCredentials allowedUser = new KeycloakCredentials("receiver", "receiverPa55");
        getKeycloakClient().createUser(defaultAddressSpace.getName(), allowedUser.getUsername(), allowedUser.getPassword(), Group.SEND_ALL.toString(), Group.RECV_ALL.toString());
        assertReceive(allowedUser.getUsername(), allowedUser.getPassword());
        getKeycloakClient().deleteUser(defaultAddressSpace.getName(), allowedUser.getUsername());

        KeycloakCredentials noAllowedUser = new KeycloakCredentials("nobody", "nobodyPa55");
        getKeycloakClient().createUser(defaultAddressSpace.getName(), noAllowedUser.getUsername(), noAllowedUser.getPassword(), Group.SEND_ALL.toString());
        assertCannotReceive(noAllowedUser.getUsername(), noAllowedUser.getPassword());
        getKeycloakClient().deleteUser(defaultAddressSpace.getName(), noAllowedUser.getUsername());
    }

    private void assertSend(String username, String password) throws Exception {
        assertTrue(canSend(queue, username, password));
        assertTrue(canSend(topic, username, password));

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertTrue(canSend(multicast, username, password));
            assertTrue(canSend(anycast, username, password));
        }
    }

    private void assertCannotSend(String username, String password) throws Exception {
        assertFalse(canSend(queue, username, password));
        assertFalse(canSend(topic, username, password));

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertFalse(canSend(multicast, username, password));
            assertFalse(canSend(anycast, username, password));
        }
    }

    private void assertReceive(String username, String password) throws Exception {
        assertTrue(canReceive(queue, username, password));
        assertTrue(canReceive(topic, username, password));

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertTrue(canReceive(multicast, username, password));
            assertTrue(canReceive(anycast, username, password));
        }
    }

    private void assertCannotReceive(String username, String password) throws Exception {
        assertFalse(canReceive(queue, username, password));
        assertFalse(canReceive(topic, username, password));

        if (getAddressSpaceType() == AddressSpaceType.STANDARD) {
            assertFalse(canReceive(multicast, username, password));
            assertFalse(canReceive(anycast, username, password));
        }
    }

    private boolean canSend(Destination destination, String username, String password) throws Exception {
        AmqpClient client = createClient(destination, username, password);
        try {
            return client.sendMessages(destination.getAddress(), Collections.singletonList("msg1"), 10, TimeUnit.SECONDS).get(30, TimeUnit.SECONDS) == 1;
        }catch (Exception ex){
            return false;
        }
    }

    private boolean canReceive(Destination destination, String username, String password) throws Exception {
        AmqpClient client = createClient(destination, username, password);
        try {
            Future<List<Message>> received = client.recvMessages(destination.getAddress(), 1, 10, TimeUnit.SECONDS);
            Future<Integer> sent = client.sendMessages(destination.getAddress(), Collections.singletonList("msg1"), 10, TimeUnit.SECONDS);
            return received.get(1, TimeUnit.MINUTES).size() == sent.get(1, TimeUnit.MINUTES);
        }catch (Exception ex){
            return false;
        }

    }

    private AmqpClient createClient(Destination dest, String username, String password) throws Exception {
        AmqpClient client = null;

        switch (dest.getType()) {
            case "queue":
                client = amqpClientFactory.createQueueClient(defaultAddressSpace);
                break;
            case "topic":
                client = amqpClientFactory.createTopicClient(defaultAddressSpace);
                break;
            case "anycast":
                client = amqpClientFactory.createQueueClient(defaultAddressSpace);
                break;
            case "multicast":
                client = amqpClientFactory.createBroadcastClient(defaultAddressSpace);
                break;
        }

        client.getConnectOptions().setUsername(username).setPassword(password);
        return client;
    }
}
