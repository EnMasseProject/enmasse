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
import io.enmasse.systemtest.messagingclients.proton.python.PythonClientReceiver;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Tag(TestTag.isolated)
public class KornyRemoveIt extends TestBase {


    public MsgCliApiClient client;
    private static Logger log = CustomLogger.getLogger();

    @BeforeEach
    public void setup() throws MalformedURLException {
        if (client == null) {
            client = new MsgCliApiClient(kubernetes, new Endpoint("http://localhost:4242"), "");
        }
    }

    @Test
    void testClientContainer() throws InterruptedException, ExecutionException, TimeoutException {
        PythonClientReceiver msgClient = new PythonClientReceiver();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, "fake_url");
        arguments.put(ClientArgument.ADDRESS, "fake_address");
        arguments.put(ClientArgument.COUNT, "1");
        msgClient.setArguments(arguments);

        List<String> apiArgument = Arrays.asList(ClientType.getCommand(msgClient.getClientType()));
        apiArgument.addAll(msgClient.getArguments());

        JsonObject response = client.startClients(apiArgument, 1);

        log.info(response.toString());
        Thread.sleep(5000);

        String uuid = response.getString("id");

        response = client.getClientInfo(uuid);
        log.info(response.toString());
    }
}
