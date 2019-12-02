/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.clients;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestBaseShared;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.messagingclients.mqtt.PahoMQTTClientReceiver;
import io.enmasse.systemtest.messagingclients.mqtt.PahoMQTTClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExternalClients
public abstract class ClusterClientTestBase extends TestBase implements ITestBaseShared {
    private ClientArgumentMap arguments = new ClientArgumentMap();

    @BeforeEach
    public void setUpClientBase() throws Exception {
        arguments.put(ClientArgument.USERNAME, defaultCredentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, defaultCredentials.getPassword());
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.CONN_SSL, "true");
    }

    private Endpoint getMessagingRoute(AddressSpace addressSpace, boolean websocket, boolean ssl, boolean mqtt) throws Exception {
        int port = ssl ? 5671 : 5672;
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) && mqtt) {
            port = ssl ? 8883 : 1883;
        }
        return new Endpoint(String.format("%s-%s.%s.svc.cluster.local",
                (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) && mqtt) ? "mqtt" : "messaging",
                AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace),
                environment.namespace()),
                websocket && addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) ? 443 : port);
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        doBasicMessageTest(sender, receiver, false);
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver, boolean websocket) throws Exception {
        int expectedMsgCount = 10;

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "message-basic-" + ClientType.getAddressName(sender) + "-" + (websocket ? "ws" : "")))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("message-basic-" + ClientType.getAddressName(sender) + "-" + (websocket ? "ws" : ""))
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        arguments.put(ClientArgument.BROKER, getMessagingRoute(getSharedAddressSpace(), websocket, true, false).toString());
        arguments.put(ClientArgument.ADDRESS, dest.getSpec().getAddress());
        arguments.put(ClientArgument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(ClientArgument.MSG_CONTENT, "message");
        if (websocket) {
            arguments.put(ClientArgument.CONN_WEB_SOCKET, "true");
            if (getSharedAddressSpace().getSpec().getType().equals(AddressSpaceType.STANDARD.toString())) {
                arguments.put(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS, "binary");
            }
        }

        sender.setArguments(arguments);
        arguments.put(ClientArgument.TIMEOUT, "10");  // In seconds, maximum time the consumer waits for a single message
        arguments.remove(ClientArgument.MSG_CONTENT);
        receiver.setArguments(arguments);

        assertTrue(sender.run());
        assertTrue(receiver.run());

        assertEquals(expectedMsgCount, sender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiver.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    protected void doMqttMessageTest() throws Exception {
        int expectedMsgCount = 10;
        AbstractClient sender = new PahoMQTTClientSender();
        AbstractClient receiver = new PahoMQTTClientReceiver();

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "basic-mqtt-" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("basic-mqtt-" + ClientType.getAddressName(sender))
                .withPlan(getSharedAddressSpace().getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) ? DestinationPlan.STANDARD_LARGE_TOPIC : getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();

        resourcesManager.setAddresses(dest);

        arguments.put(ClientArgument.BROKER, getMessagingRoute(getSharedAddressSpace(), false, false, true).toString());
        arguments.put(ClientArgument.ADDRESS, dest.getSpec().getAddress());
        arguments.put(ClientArgument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(ClientArgument.MSG_CONTENT, "message");
        arguments.put(ClientArgument.TIMEOUT, "20");
        arguments.remove(ClientArgument.CONN_SSL);


        sender.setArguments(arguments);
        arguments.remove(ClientArgument.MSG_CONTENT);
        arguments.put(ClientArgument.TIMEOUT, "40");
        receiver.setArguments(arguments);

        Future<Boolean> recResult = receiver.runAsync();
        Thread.sleep(20_000);

        assertAll(
                () -> assertTrue(sender.run(), "Producer failed, expected return code 0"),
                () -> assertEquals(expectedMsgCount, sender.getMessages().size(),
                        String.format("Expected %d sent messages", expectedMsgCount)));
        assertAll(
                () -> assertTrue(recResult.get(), "Subscriber failed, expected return code 0"),
                () -> assertEquals(expectedMsgCount, receiver.getMessages().size(),
                        String.format("Expected %d received messages", expectedMsgCount)));
    }
}
