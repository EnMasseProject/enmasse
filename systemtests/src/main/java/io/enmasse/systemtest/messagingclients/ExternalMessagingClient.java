/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.messagingclients;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.vertx.core.json.JsonArray;

import java.util.Objects;
import java.util.concurrent.Future;

public class ExternalMessagingClient {
    private AbstractClient client;
    private ClientArgumentMap arguments;

    public ExternalMessagingClient() {
        this.arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.CONN_SSL, "true");
    }

    public ExternalMessagingClient withClientEngine(AbstractClient clientEngine) {
        this.client = clientEngine;
        return this;
    }

    public ExternalMessagingClient withArguments(ClientArgumentMap arguments) {
        this.arguments = arguments;
        return this;
    }

    public ExternalMessagingClient withAdditionalArgument(ClientArgument argName, Object value) {
        Objects.requireNonNull(value);
        this.arguments.put(argName, String.valueOf(value));
        return this;
    }


    public ExternalMessagingClient withMessagingRoute(String route) {
        this.arguments.put(ClientArgument.BROKER, route);
        return this;
    }

    public ExternalMessagingClient withMessagingRoute(Endpoint route) {
        this.arguments.put(ClientArgument.BROKER, route.toString());
        return this;
    }

    public ExternalMessagingClient withAddress(Address address) {
        this.arguments.put(ClientArgument.ADDRESS, address.getSpec().getAddress());
        return this;
    }

    public ExternalMessagingClient withCredentials(UserCredentials credentials) {
        this.arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        this.arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        return this;
    }

    public ExternalMessagingClient withCredentials(String username, String password) {
        this.arguments.put(ClientArgument.USERNAME, username);
        this.arguments.put(ClientArgument.PASSWORD, password);
        return this;
    }

    public ExternalMessagingClient withMessageBody(Object body) {
        this.arguments.put(ClientArgument.MSG_CONTENT, String.valueOf(body));
        return this;
    }

    public ExternalMessagingClient withCount(int count) {
        this.arguments.put(ClientArgument.COUNT, String.valueOf(count));
        return this;
    }

    public ExternalMessagingClient withTimeout(int timeout) {
        this.arguments.put(ClientArgument.TIMEOUT, String.valueOf(timeout));
        return this;
    }

    //===================================================================
    //                          Content methods
    //===================================================================

    public String getStdOutput() {
        Objects.requireNonNull(this.client);
        return client.getStdOut();
    }

    public String getStdError() {
        Objects.requireNonNull(this.client);
        return client.getStdErr();
    }

    public JsonArray getMessages() {
        Objects.requireNonNull(this.client);
        return client.getMessages();
    }


    //===================================================================
    //                          Run methods
    //===================================================================

    public boolean run(boolean logToOutput) {
        return run(120_000, logToOutput);
    }

    public boolean run(int timeout) {
        return run(timeout, true);
    }

    public boolean run() {
        return run(120_000, true);
    }

    public boolean run(int timeout, boolean logToOutput) {
        this.client.setArguments(arguments);
        return this.client.run(timeout, logToOutput);
    }

    public Future<Boolean> runAsync() {
        return runAsync(true);
    }

    public Future<Boolean> runAsync(boolean logToOutput) {
        this.client.setArguments(arguments);
        return this.client.runAsync(logToOutput);
    }
}
