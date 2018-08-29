/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.clients;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.MsgCliApiClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public abstract class ClusterClientTestBase extends TestBaseWithShared {
    private ClientArgumentMap arguments = new ClientArgumentMap();
    private Logger log = CustomLogger.getLogger();
    private MsgCliApiClient cliApiClient;

    @BeforeEach
    public void setUpClientBase() throws Exception {
        if (cliApiClient == null) {
            Endpoint cliEndpoint = TestUtils.deployMessagingClientApp(environment.namespace(), kubernetes);
            cliApiClient = new MsgCliApiClient(kubernetes, cliEndpoint);
        }

        arguments.put(ClientArgument.USERNAME, defaultCredentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, defaultCredentials.getPassword());
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.CONN_SSL, "true");
    }

    @AfterAll
    public void tearDownAll() {
        TestUtils.deleteMessagingClientApp(environment.namespace(), kubernetes);
    }

    private Endpoint getMessagingRoute(AddressSpace addressSpace, boolean websocket) {
        return new Endpoint(String.format("messaging-%s.%s.svc",
                addressSpace.getInfraUuid(), environment.namespace()), websocket && addressSpace.getType().equals(AddressSpaceType.STANDARD) ? 443 : 5671);
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        doBasicMessageTest(sender, receiver, false);
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver, boolean websocket) throws Exception {
        int expectedMsgCount = 10;

        Destination dest = Destination.queue("message-basic" + ClientType.getAddressName(sender),
                getDefaultPlan(AddressType.QUEUE));
        setAddresses(dest);

        arguments.put(ClientArgument.BROKER, getMessagingRoute(sharedAddressSpace, websocket).toString());
        arguments.put(ClientArgument.ADDRESS, dest.getAddress());
        arguments.put(ClientArgument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(ClientArgument.MSG_CONTENT, "msg no. %d");
        if (websocket) {
            arguments.put(ClientArgument.CONN_WEB_SOCKET, "true");
            if (sharedAddressSpace.getType() == AddressSpaceType.STANDARD) {
                arguments.put(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS, "binary");
            }
        }

        sender.setArguments(arguments);
        arguments.remove(ClientArgument.MSG_CONTENT);
        receiver.setArguments(arguments);

        JsonObject response = cliApiClient.sendAndGetStatus(sender);
        assertThat(response.getInteger("ecode"), is(0));

        response = cliApiClient.sendAndGetStatus(receiver);
        assertThat(response.getInteger("ecode"), is(0));
    }
}
