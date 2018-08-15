/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.apiclients.MsgCliApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Tag(TestTag.isolated)
public class KornyRemoveIt extends TestBase {


    public MsgCliApiClient client;
    private static Logger log = CustomLogger.getLogger();

    @BeforeAll
    public void setup() throws Exception {
        kubernetes.createPodFromTemplate("kornys", "/messaging-clients.yaml");
        if (client == null) {
            client = new MsgCliApiClient(kubernetes, new Endpoint(String.format("http://%s:4242", kubernetes.getPodIp("kornys", "messaging-clients"))), "");
        }
    }

    @AfterAll
    public void tearDown() throws Exception {
        kubernetes.deletePod("kornys", "messaging-clients");
    }

    @Test
    void testClientContainer() throws InterruptedException, ExecutionException, TimeoutException {
        RheaClientSender msgClient = new RheaClientSender();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, "fake_url");
        arguments.put(ClientArgument.ADDRESS, "fake_address");
        arguments.put(ClientArgument.COUNT, "1");
        arguments.put(ClientArgument.CONN_RECONNECT, "false");
        msgClient.setArguments(arguments);


        List<String> apiArgument = new LinkedList<>();
        apiArgument.add(ClientType.getCommand(msgClient.getClientType()));
        apiArgument.addAll(msgClient.getArguments());

        JsonObject response = client.startClients(apiArgument, 1);

        log.info(response.toString());
        Thread.sleep(5000);

        JsonArray ids = response.getJsonArray("clients");
        String uuid = ids.getString(0);

        response = client.getClientInfo(uuid);
        log.info(response.toString());
    }
}
