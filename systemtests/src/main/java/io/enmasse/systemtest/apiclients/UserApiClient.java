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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import javax.ws.rs.core.HttpHeaders;
import java.net.MalformedURLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UserApiClient extends ApiClient {

    private static Logger log = CustomLogger.getLogger();
    private final int initRetry = 10;
    private final String userPathPattern;

    public UserApiClient(Kubernetes kubernetes) throws MalformedURLException {
        super(kubernetes, kubernetes.getRestEndpoint(), "user.enmasse.io/v1alpha1");
        this.userPathPattern = "/apis/user.enmasse.io/v1alpha1/namespaces/%s/messagingusers";
    }

    public void createUser(String addressSpace, String namespace, UserCredentials credentials) throws Exception {
        User user = new User()
                .setUsername(credentials.getUsername())
                .setPassword(credentials.getPassword())
                .addAuthorization(new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation("send")
                        .addOperation("recv")
                        .addOperation("view"))
                .addAuthorization(new User.AuthorizationRule()
                        .addOperation("manage"));
        createUser(addressSpace, namespace, user);
    }
    public void createUser(String addressSpace, String namespace, User user) throws Exception {
        createUser(addressSpace, namespace, user, HTTP_CREATED);
    }

    public void createUser(String addressSpace, String namespace, User user, int expectedCode) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.CREATE_USER);
        try {
            JsonObject config = new JsonObject();
            JsonObject metadata = new JsonObject();
            metadata.put("name", addressSpace + "." + user.getUsername());
            metadata.put("namespace", namespace);
            config.put("metadata", metadata);

            JsonObject spec = new JsonObject();
            spec.put("username", user.getUsername());
            JsonObject authentication = new JsonObject();
            authentication.put("type", "password");
            authentication.put("password", user.getPassword());
            spec.put("authentication", authentication);

            JsonArray authorization = new JsonArray();

            for (User.AuthorizationRule rule : user.getAuthorization()) {
                JsonObject authz = new JsonObject();
                JsonArray addresses = new JsonArray();
                JsonArray operations = new JsonArray();

                for (String address : rule.getAddresses()) {
                    addresses.add(address);
                }

                for (String operation : rule.getOperations()) {
                    operations.add(operation);
                }

                authz.put("addresses", addresses);
                authz.put("operations", operations);
                authorization.add(authz);
            }
            spec.put("authorization", authorization);

            String userPath = String.format(userPathPattern, namespace);
            log.info("POST-user: path {}; body {}", userPath, config.toString());
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

            doRequestNTimes(initRetry, () -> {
                        client.post(endpoint.getPort(), endpoint.getHost(), userPath)
                                .timeout(20_000)
                                .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                                .as(BodyCodec.jsonObject())
                                .sendJsonObject(config, ar -> responseHandler(ar,
                                        responsePromise,
                                        expectedCode,
                                        String.format("Error: create address space '%s'", addressSpace)));
                        return responsePromise.get(30, TimeUnit.SECONDS);
                    },
                    Optional.of(() -> kubernetes.getRestEndpoint()),
                    Optional.empty());
        }finally {
            TimeMeasuringSystem.stopOperation(operationID);
        }
    }

    public void deleteUser(String addressSpace, String namespace, String userName) throws Exception {
        deleteUser(addressSpace, namespace, userName, HTTP_OK);
    }

    public void deleteUser(String addressSpace, String namespace, String userName, int expectedCode) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.DELETE_USER);
        try {
            String path = String.format(userPathPattern, namespace) + "/" + String.format("%s.%s", addressSpace, userName);
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
                                        String.format("Error: delete address space '%s'", addressSpace)));
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
