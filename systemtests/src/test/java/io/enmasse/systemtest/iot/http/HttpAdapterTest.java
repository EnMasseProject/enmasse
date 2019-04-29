/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.apiclients.Predicates;
import io.enmasse.systemtest.bases.IoTTestBaseWithShared;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageType;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(sharedIot)
public class HttpAdapterTest extends IoTTestBaseWithShared {

    private Logger log = CustomLogger.getLogger();

    private Endpoint deviceRegistryEndpoint;
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;
    private AmqpClient businessApplicationClient;
    private HttpAdapterClient adapterClient;

    private final String deviceId = UUID.randomUUID().toString();
    private final String deviceAuthId = UUID.randomUUID().toString();
    private final String devicePassword = UUID.randomUUID().toString();
    private final String businessApplicationUsername = UUID.randomUUID().toString();
    private final String businessApplicationPassword = UUID.randomUUID().toString();

    @BeforeEach
    void initEnv() throws Exception {
        deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        registryClient = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint);
        registryClient.registerDevice(tenantId(), deviceId);
        credentialsClient.addCredentials(tenantId(), deviceId, deviceAuthId, devicePassword);

        User businessApplicationUser = UserUtils.createUserResource(new UserCredentials(businessApplicationUsername, businessApplicationPassword))
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withAddresses(IOT_ADDRESS_TELEMETRY + "/" + tenantId(),
                                        IOT_ADDRESS_TELEMETRY + "/" + tenantId() + "/*",
                                        IOT_ADDRESS_EVENT + "/" + tenantId(),
                                        IOT_ADDRESS_EVENT + "/" + tenantId() + "/*")
                                .withOperations(Operation.recv)
                                .build()))
                .endSpec()
                .done();

        AddressSpace addressSpace = getAddressSpace(iotProjectNamespace, sharedProject.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());

        createOrUpdateUser(addressSpace, businessApplicationUser);

        businessApplicationClient = amqpClientFactory.createQueueClient(addressSpace);
        businessApplicationClient.getConnectOptions()
                .setUsername(businessApplicationUsername)
                .setPassword(businessApplicationPassword);

        Endpoint httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        adapterClient = new HttpAdapterClient(kubernetes, httpAdapterEndpoint, deviceAuthId + "@" + tenantId(), devicePassword);

    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectHttpAdapterQdrProxyState();
        }
        credentialsClient.deleteAllCredentials(tenantId(), deviceId);
        credentialsClient.getCredentials(tenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        registryClient.deleteDeviceRegistration(tenantId(), deviceId);
        registryClient.getDeviceRegistration(tenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        businessApplicationClient.close();
        adapterClient.close();
        removeUser(getAddressSpace(), businessApplicationUsername);
    }

    @Test
    public void basicTelemetryTest() throws Exception {

        log.info("Consuming one telemetry message in business application");
        CountDownLatch latch = new CountDownLatch(1);
        businessApplicationClient.recvMessages(IOT_ADDRESS_TELEMETRY + "/" + tenantId(), msg -> {
            latch.countDown();
            return true;
        });

        waitForFirstSuccess(adapterClient, MessageType.TELEMETRY);

        latch.await(30, TimeUnit.SECONDS);
        log.info("Telemetry successfully consumed");

    }

    @Test
    public void batchTelemetryTest() throws Exception {

        int messagesToSend = 50;

        log.info("Connecting amqp consumer");
        AtomicInteger receivedMessagesCounter = new AtomicInteger(0);
        Future<List<Message>> futureReceivedMessages = setUpMessagingConsumer(IOT_ADDRESS_TELEMETRY, receivedMessagesCounter, messagesToSend);

        waitForFirstSuccess(adapterClient, MessageType.TELEMETRY);

        log.info("Sending telemetry messages");
        for (int i = 0; i < messagesToSend; i++) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            sendWithRetry(MessageType.TELEMETRY, json);
        }

        try {
            log.info("Waiting to receive telemetry data in business application");
            futureReceivedMessages.get(60, TimeUnit.SECONDS);
            assertEquals(messagesToSend, receivedMessagesCounter.get());
            log.info("Telemetry successfully consumed");
        } catch (TimeoutException e) {
            log.error("Timeout receiving telemetry, messages received: {} error:", receivedMessagesCounter.get(), e);
            throw e;
        }

    }

    @Test
    public void basicEventTest() throws Exception {

        waitForFirstSuccess(adapterClient, MessageType.EVENT);

        log.info("Consuming one event in business application");
        CountDownLatch latch = new CountDownLatch(1);
        businessApplicationClient.recvMessages(IOT_ADDRESS_EVENT + "/" + tenantId(), msg -> {
            latch.countDown();
            return true;
        });

        latch.await(30, TimeUnit.SECONDS);
        log.info("Event successfully consumed");

    }

    @Test
    public void batchEventTest() throws Exception {

        waitForFirstSuccess(adapterClient, MessageType.EVENT);

        int eventsToSend = 5;
        log.info("Sending events");
        for (int i = 0; i < eventsToSend; i++) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            sendWithRetry(MessageType.EVENT, json);
        }

        log.info("Consuming events in business application");
        AtomicInteger receivedMessagesCounter = new AtomicInteger(0);
        Future<List<Message>> status = setUpMessagingConsumer(IOT_ADDRESS_EVENT, receivedMessagesCounter, eventsToSend);

        try {
            log.info("Waiting to receive events");
            status.get(60, TimeUnit.SECONDS);
            assertEquals(eventsToSend, receivedMessagesCounter.get());
            log.info("Events successfully consumed");
        } catch (TimeoutException e) {
            log.error("Timeout receiving events, messages received: {} error:", receivedMessagesCounter.get(), e);
            throw e;
        }

    }

    private Future<List<Message>> setUpMessagingConsumer(String type, AtomicInteger receivedMessagesCounter, int expectedMessages) {
        return businessApplicationClient.recvMessages(type + "/" + tenantId(), msg -> {
            if (msg.getBody() instanceof Data) {
                Binary value = ((Data) msg.getBody()).getValue();
                JsonObject json = new JsonObject(Buffer.buffer(value.getArray()));
                if (json.containsKey("i")) {
                    return receivedMessagesCounter.incrementAndGet() == expectedMessages;
                }
            }
            return false;
        });
    }

    private void sendWithRetry(MessageType messageType, JsonObject json) throws Exception, InterruptedException {
        if (messageType == MessageType.COMMAND_RESPONSE) {
            return;
        }
        var timeout = new TimeoutBudget(30, TimeUnit.SECONDS);
        var sendOk = false;
        while (!timeout.timeoutExpired() && !sendOk) {
            var response = messageType == MessageType.TELEMETRY ? adapterClient.sendTelemetry(json, Predicates.any()) : adapterClient.sendEvent(json, Predicates.any());
            if (Predicates.notIn(HTTP_ACCEPTED, HTTP_UNAVAILABLE).test(response.statusCode())) {
                log.error("expected-code: {}, response-code: {}, body: {}, op: {}", HTTP_ACCEPTED, response.statusCode(), response.body(), "Error sending " + messageType.name().toLowerCase() + " data");
                throw new RuntimeException("Status " + response.statusCode() + " body: " + (response.body() != null ? response.body().toString() : null));
            }
            sendOk = response.statusCode() == HTTP_ACCEPTED;
            Thread.sleep(1000);
        }
    }

}

