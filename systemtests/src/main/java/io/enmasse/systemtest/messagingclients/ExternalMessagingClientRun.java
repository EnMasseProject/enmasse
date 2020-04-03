/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients;

import java.util.concurrent.Future;

public class ExternalMessagingClientRun {

    private Future<Boolean> result;
    private ExternalMessagingClient client;
    private ReceiverTester receiverTester;
    private String descriptor;

    public static ExternalMessagingClientRun of(ExternalMessagingClient client) {
        return of(client, null);
    }

    public static ExternalMessagingClientRun of(ExternalMessagingClient client, ReceiverTester receiverTester) {
        return of(client, receiverTester, null);
    }

    public static ExternalMessagingClientRun of(ExternalMessagingClient client, ReceiverTester receiverTester, String descriptor) {
        ExternalMessagingClientRun run = new ExternalMessagingClientRun();
        run.client = client;
        run.receiverTester = receiverTester;
        run.descriptor = descriptor;
        return run;
    }

    public void runAsync() {
        result = client.runAsync();
    }

    public Future<Boolean> getResult() {
        return result;
    }

    public ExternalMessagingClient getClient() {
        return client;
    }

    public ReceiverTester getReceiverTester() {
        return receiverTester;
    }

    public String getDescriptor() {
        return descriptor;
    }

}
