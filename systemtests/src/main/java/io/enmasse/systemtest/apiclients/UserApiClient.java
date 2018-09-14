/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.User;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.timemeasuring.Operation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

public class UserApiClient extends ApiClient {

    private static Logger log = CustomLogger.getLogger();
    private final int initRetry = 10;
    private final String userPath;

    public UserApiClient(Kubernetes kubernetes) throws MalformedURLException {
        super(kubernetes, kubernetes.getRestEndpoint(), "user.enmasse.io/v1alpha1");
        this.userPath = String.format("/apis/user.enmasse.io/v1alpha1/namespaces/%s/messagingusers", kubernetes.getNamespace());
    }

    public UserApiClient(Kubernetes kubernetes, String namespace) throws MalformedURLException {
        super(kubernetes, kubernetes.getRestEndpoint(), "user.enmasse.io/v1alpha1");
        this.userPath = String.format("/apis/user.enmasse.io/v1alpha1/namespaces/%s/messagingusers", namespace);
    }

    public JsonObject getUser(String addressSpace, String userName) throws Exception {
        return getUser(addressSpace, userName, HTTP_OK);
    }

    public JsonObject getUser(String addressSpace, String userName, int expectedCode) throws Exception {
        String path = userPath + "/" + String.format("%s.%s", addressSpace, userName);
        log.info("GET-user: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
                    client.get(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: get user '%s' in addressspace '%s'", userName, addressSpace)));
                    return responsePromise.get(2, TimeUnit.MINUTES);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public JsonObject getUserList(String addressSpace) throws Exception {
        return getUserList(addressSpace, HTTP_OK);
    }

    public JsonObject getUserList(String addressSpace, int expectedCode) throws Exception {
        String path = userPath;
        log.info("GET-user-list: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
                    client.get(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: get user list for address space '%s'", addressSpace)));
                    return responsePromise.get(2, TimeUnit.MINUTES);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public JsonObject createUser(String addressSpace, UserCredentials credentials) throws Exception {
        User user = new User()
                .setUserCredentials(credentials)
                .addAuthorization(new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE)
                        .addOperation(User.Operation.VIEW))
                .addAuthorization(new User.AuthorizationRule()
                        .addOperation(User.Operation.MANAGE));
        return createUser(addressSpace, user, HTTP_CREATED);
    }

    public JsonObject createUser(String addressSpace, User user) throws Exception {
        return createUser(addressSpace, user, HTTP_CREATED);
    }

    public JsonObject createUser(String addressSpace, User user, int expectedCode) throws Exception {
        return createUser(addressSpace, user.toJson(addressSpace), HTTP_CREATED);
    }

    public JsonObject createUser(String addressSpace, JsonObject userPayLoad, int expectedCode) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.CREATE_USER);
        try {
            log.info("POST-user: path {}; body {}", userPath, userPayLoad.toString());
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

            return doRequestNTimes(initRetry, () -> {
                        client.post(endpoint.getPort(), endpoint.getHost(), userPath)
                                .timeout(20_000)
                                .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                                .as(BodyCodec.jsonObject())
                                .sendJsonObject(userPayLoad, ar -> responseHandler(ar,
                                        responsePromise,
                                        expectedCode,
                                        String.format("Error: create user '%s'", userPayLoad.getJsonObject("spec").getString("username"))));
                        return responsePromise.get(30, TimeUnit.SECONDS);
                    },
                    Optional.of(() -> kubernetes.getRestEndpoint()),
                    Optional.empty());
        } finally {
            TimeMeasuringSystem.stopOperation(operationID);
        }
    }

    public void deleteUser(String addressSpace, String userName) throws Exception {
        deleteUser(addressSpace, userName, HTTP_OK);
    }

    public void deleteUser(String addressSpace, String userName, int expectedCode) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.DELETE_USER);
        try {
            String path = userPath + "/" + String.format("%s.%s", addressSpace, userName);
            log.info("DELETE-user: path '{}'", path);
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
            doRequestNTimes(initRetry, () -> {
                        client.delete(endpoint.getPort(), endpoint.getHost(), path)
                                .as(BodyCodec.jsonObject())
                                .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                                .timeout(20_000)
                                .send(ar -> responseHandler(ar,
                                        responsePromise,
                                        expectedCode,
                                        String.format("Error: delete user '%s'", userName)));
                        return responsePromise.get(2, TimeUnit.MINUTES);
                    },
                    Optional.of(() -> kubernetes.getRestEndpoint()),
                    Optional.empty());
        } finally {
            TimeMeasuringSystem.stopOperation(operationID);
        }
    }

    public JsonObject updateUser(String addressSpace, User user) throws Exception {
        return updateUser(addressSpace, user, HTTP_OK);
    }

    public JsonObject updateUser(String addressSpace, User user, int expectedCode) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.UPDATE_USER);
        try {
            String path = userPath + "/" + String.format("%s.%s", addressSpace, user.getUsername());
            JsonObject payload = user.toJson(addressSpace);
            log.info("PUT-user: path '{}', data: {}", path, payload.toString());
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
            return doRequestNTimes(initRetry, () -> {
                        client.put(endpoint.getPort(), endpoint.getHost(), path)
                                .as(BodyCodec.jsonObject())
                                .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                                .timeout(20_000)
                                .as(BodyCodec.jsonObject())
                                .sendJsonObject(payload, ar -> responseHandler(ar,
                                        responsePromise,
                                        expectedCode,
                                        String.format("Error: update user '%s'", user.getUsername())));
                        return responsePromise.get(2, TimeUnit.MINUTES);
                    },
                    Optional.of(() -> kubernetes.getRestEndpoint()),
                    Optional.empty());
        } finally {
            TimeMeasuringSystem.stopOperation(operationID);
        }
    }

    @Override
    protected String apiClientName() {
        return "user-api";
    }
}
