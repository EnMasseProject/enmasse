/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.clients;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.MsgCliApiClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.mqtt.PahoMQTTClientReceiver;
import io.enmasse.systemtest.messagingclients.mqtt.PahoMQTTClientSender;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

public abstract class ClusterClientTestBase extends TestBaseWithShared {
    private ClientArgumentMap arguments = new ClientArgumentMap();
    private Logger log = CustomLogger.getLogger();
    private MsgCliApiClient cliApiClient;

    @BeforeEach
    public void setUpClientBase() throws Exception {
        if (cliApiClient == null) {
            SystemtestsKubernetesApps.deployMessagingClientApp(environment.namespace(), kubernetes);
            cliApiClient = new MsgCliApiClient(kubernetes, SystemtestsKubernetesApps.getMessagingClientEndpoint(environment.namespace(), kubernetes));
        }

        arguments.put(ClientArgument.USERNAME, defaultCredentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, defaultCredentials.getPassword());
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.CONN_SSL, "true");
    }

    @AfterAll
    public void tearDownAll() {
        SystemtestsKubernetesApps.deleteMessagingClientApp(environment.namespace(), kubernetes);
    }

    private Endpoint getMessagingRoute(AddressSpace addressSpace, boolean websocket, boolean ssl, boolean mqtt) throws Exception {
        int port = ssl ? 5671 : 5672;
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) && mqtt) {
            port = ssl ? 8883 : 1883;
        }
        return new Endpoint(String.format("%s-%s.%s.svc",
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

        Address dest = AddressUtils.createQueueAddressObject("message-basic" + ClientType.getAddressName(sender),
                getDefaultPlan(AddressType.QUEUE));
        setAddresses(dest);

        arguments.put(ClientArgument.BROKER, getMessagingRoute(sharedAddressSpace, websocket, true, false).toString());
        arguments.put(ClientArgument.ADDRESS, dest.getSpec().getAddress());
        arguments.put(ClientArgument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(ClientArgument.MSG_CONTENT, "message");
        if (websocket) {
            arguments.put(ClientArgument.CONN_WEB_SOCKET, "true");
            if (sharedAddressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString())) {
                arguments.put(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS, "binary");
            }
        }

        sender.setArguments(arguments);
        arguments.remove(ClientArgument.MSG_CONTENT);
        receiver.setArguments(arguments);

        JsonObject response = cliApiClient.sendAndGetStatus(sender);
        assertThat(String.format("Return code of sender is not 0: %s", response.toString()),
                response.getInteger("ecode"), is(0));

        Thread.sleep(2000);

        response = cliApiClient.sendAndGetStatus(receiver);
        assertThat(String.format("Return code of receiver is not 0: %s", response.toString()),
                response.getInteger("ecode"), is(0));
    }

    protected void doMqttMessageTest() throws Exception {
        int expectedMsgCount = 10;
        AbstractClient sender = new PahoMQTTClientSender();
        AbstractClient receiver = new PahoMQTTClientReceiver();

        Address dest = AddressUtils.createTopicAddressObject("message-basic-mqtt",
                sharedAddressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) ? DestinationPlan.STANDARD_LARGE_TOPIC : getDefaultPlan(AddressType.TOPIC));
        setAddresses(dest);

        arguments.put(ClientArgument.BROKER, getMessagingRoute(sharedAddressSpace, false, false, true).toString());
        arguments.put(ClientArgument.ADDRESS, dest.getSpec().getAddress());
        arguments.put(ClientArgument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(ClientArgument.MSG_CONTENT, "message");
        arguments.put(ClientArgument.TIMEOUT, "20");
        arguments.remove(ClientArgument.CONN_SSL);


        sender.setArguments(arguments);
        arguments.remove(ClientArgument.MSG_CONTENT);
        receiver.setArguments(arguments);

        log.info("Subscribe receiver");
        String receiverId = cliApiClient.sendAndGetId(receiver);

        if (isBrokered(sharedAddressSpace)) {
            waitForSubscribers(new ArtemisManagement(), sharedAddressSpace, dest.getSpec().getAddress(), 1);
        } else {
            Thread.sleep(10_000); //mqtt connection is not in console
        }

        log.info("Send messages");
        JsonObject response = cliApiClient.sendAndGetStatus(sender);
        assertThat(String.format("Return code of sender is not 0: %s", response.toString()),
                response.getInteger("ecode"), is(0));

        Thread.sleep(10_000);

        log.info("Check if subscriber received messages");
        response = cliApiClient.getClientInfo(receiverId);
        log.info(response.toString());
        assertThat(String.format("Return code of receiver is not 0: %s", response.toString()),
                response.getInteger("ecode"), is(0));
        assertFalse(response.getString("stdOut").isEmpty(), "Receiver does not receive message");
    }
}
