/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserCrd;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
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

    private static final String USERS_PATH = "/apis/user.enmasse.io/" + UserCrd.VERSION + "/messagingusers";

    public UserApiClient(Kubernetes kubernetes) throws MalformedURLException {
        super(kubernetes, kubernetes::getRestEndpoint, "user.enmasse.io/" + UserCrd.VERSION);
        this.userPath = String.format("/apis/user.enmasse.io/%s/namespaces/%s/messagingusers", UserCrd.VERSION, kubernetes.getInfraNamespace());
    }

    public UserApiClient(Kubernetes kubernetes, String namespace) throws MalformedURLException {
        super(kubernetes, kubernetes::getRestEndpoint, "user.enmasse.io/" + UserCrd.VERSION);
        this.userPath = String.format("/apis/user.enmasse.io/%s/namespaces/%s/messagingusers", UserCrd.VERSION, namespace);
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
        User user = UserUtils.createUserObject(credentials);
        return createUser(addressSpace, user, HTTP_CREATED);
    }

    public JsonObject createUser(String addressSpace, User user) throws Exception {
        return createUser(addressSpace, user, HTTP_CREATED);
    }

    public JsonObject createUser(String addressSpace, User user, int expectedCode) throws Exception {
        return createUser(UserUtils.userToJson(addressSpace, user), expectedCode);
    }

    public JsonObject createUser(JsonObject userPayLoad, int expectedCode) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_USER);
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
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_USER);
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
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.UPDATE_USER);
        try {
            String path = userPath + "/" + String.format("%s.%s", addressSpace, user.getSpec().getUsername());
            JsonObject payload = UserUtils.userToJson(addressSpace, user);
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
                                        String.format("Error: update user '%s'", user.getSpec().getUsername())));
                        return responsePromise.get(2, TimeUnit.MINUTES);
                    },
                    Optional.of(() -> kubernetes.getRestEndpoint()),
                    Optional.empty());
        } finally {
            TimeMeasuringSystem.stopOperation(operationID);
        }
    }

    /**
     * Get all users (non-namespaced)
     *
     * @param expectedCode the expected return code
     * @return the result
     * @throws Exception if anything goes wrong
     */
    public JsonObject getAllUsers(final int expectedCode) throws Exception {
        log.info("GET-all-users: path {}; ", USERS_PATH);
        return doRequestNTimes(initRetry, () -> {
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
            HttpRequest<JsonObject> request = client.get(endpoint.getPort(), endpoint.getHost(), USERS_PATH)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject())
                    .timeout(20_000);
            request.send(ar -> responseHandler(ar, responsePromise, expectedCode, "Error: get users"));
            return responsePromise.get(30, TimeUnit.SECONDS);
        }, Optional.of(() -> kubernetes.getRestEndpoint()), Optional.empty());
    }

    /**
     * Get all users (non-namespaced)
     *
     * @return the result
     * @throws Exception if anything goes wrong
     */
    public JsonObject getAllUsers() throws Exception {
        return getAllUsers(HTTP_OK);
    }

    @Override
    protected String apiClientName() {
        return "user-api";
    }
}
