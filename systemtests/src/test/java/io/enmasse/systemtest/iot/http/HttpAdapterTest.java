/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.http;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TimeoutBudget.ofDuration;
import static io.enmasse.systemtest.apiclients.Predicates.any;
import static io.enmasse.systemtest.apiclients.Predicates.is;
import static io.enmasse.systemtest.iot.MessageType.TELEMETRY;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.IoTTestBaseWithShared;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageType;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@Tag(sharedIot)
public class HttpAdapterTest extends IoTTestBaseWithShared {

    private Logger log = CustomLogger.getLogger();

    private Endpoint deviceRegistryEndpoint;
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;
    private AmqpClient businessApplicationClient;
    private HttpAdapterClient adapterClient;

    private String deviceId = "1234";
    private String deviceAuthId = "sensor1234";
    private String devicePassword = "devicePwd1234";
    private String businessApplicationUsername = "businessuser";
    private String businessApplicationPassword = "businesspwd";

    @BeforeEach
    void initEnv() throws Exception {
        deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        registryClient = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint);
        registryClient.registerDevice(tenantId(), deviceId);
        credentialsClient.addCredentials(tenantId(), deviceId, deviceAuthId, devicePassword);

        User businessApplicationUser = UserUtils.createUserObject(businessApplicationUsername, businessApplicationPassword,
                Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses(IOT_ADDRESS_TELEMETRY + "/" + tenantId(),
                                IOT_ADDRESS_TELEMETRY + "/" + tenantId() + "/*",
                                IOT_ADDRESS_EVENT + "/" + tenantId(),
                                IOT_ADDRESS_EVENT + "/" + tenantId() + "/*")
                        .withOperations(Operation.recv)
                        .build()));

        AddressSpace addressSpace = getAddressSpace(sharedProject.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());

        createUser(addressSpace, businessApplicationUser);

        businessApplicationClient = amqpClientFactory.createQueueClient(addressSpace);
        businessApplicationClient.getConnectOptions()
            .setUsername(businessApplicationUsername)
            .setPassword(businessApplicationPassword);

        Endpoint httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        adapterClient = new HttpAdapterClient(kubernetes, httpAdapterEndpoint, deviceAuthId + "@" + tenantId(), devicePassword);

    }

    @AfterEach
    void cleanEnv() throws Exception {
        credentialsClient.deleteAllCredentials(tenantId(), deviceId);
        credentialsClient.getCredentials(tenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        registryClient.deleteDeviceRegistration(tenantId(), deviceId);
        registryClient.getDeviceRegistration(tenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        businessApplicationClient.close();
        adapterClient.close();
    }

    @Test
    public void basicTelemetryTest() throws Exception {

        log.info("Connecting amqp consumer");
        AtomicInteger receivedMessagesCounter = new AtomicInteger(0);
        Future<List<Message>> futureReceivedMessages = setUpMessagingConsumer(IOT_ADDRESS_TELEMETRY, receivedMessagesCounter);

        waitForFirstSuccess(MessageType.TELEMETRY);

        int messagesToSend = 50;
        log.info("Sending telemetry messages");
        for ( int i = 0; i < messagesToSend; i++ ) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            json.put("end", i == (messagesToSend-1));
            adapterClient.send(TELEMETRY, json, is(HTTP_ACCEPTED), request -> {
                request.putHeader("QoS-Level", "1"); //at least once QoS
            }, ofSeconds(20));
        }

        try {
            log.info("Waiting to receive telemetry data in business application");
            futureReceivedMessages.get(60, TimeUnit.SECONDS);
            assertEquals(messagesToSend, receivedMessagesCounter.get());
        }catch(TimeoutException e) {
            log.error("Timeout receiving telemetry, messages received: {} error:", receivedMessagesCounter.get(), e);
            throw e;
        }

    }

    @Test
    public void basicEventTest() throws Exception {

        waitForFirstSuccess(MessageType.EVENT);

        int eventsToSend = 5;
        log.info("Sending events");
        for ( int i = 0; i < eventsToSend; i++ ) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            json.put("end", i == (eventsToSend-1));
            adapterClient.sendEvent(json, is(HTTP_ACCEPTED));
        }

        log.info("Consuming events in business application");
        AtomicInteger receivedMessagesCounter = new AtomicInteger(0);
        Future<List<Message>> status = setUpMessagingConsumer(IOT_ADDRESS_EVENT, receivedMessagesCounter);

        try {
            log.info("Waiting to receive events");
            status.get(60, TimeUnit.SECONDS);
            assertEquals(eventsToSend, receivedMessagesCounter.get());
        }catch(TimeoutException e) {
            log.error("Timeout receiving events, messages received: {} error:", receivedMessagesCounter.get(), e);
            throw e;
        }

    }

    private void waitForFirstSuccess(MessageType type) throws Exception {
        JsonObject json = new JsonObject(Map.of("a", "b"));
        TestUtils.waitUntilCondition("First successful "+type.name().toLowerCase()+" message", () -> {
            try {
                if(type == MessageType.EVENT) {
                    var response = adapterClient.sendEvent(json, any());
                    return response.statusCode() == HTTP_ACCEPTED;
                } else if(type == MessageType.TELEMETRY) {
                    var response = adapterClient.sendTelemetry(json, any());
                    return response.statusCode() == HTTP_ACCEPTED;
                } else {
                    return true;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, ofDuration(ofSeconds(60)));

        log.info("First "+type.name().toLowerCase()+" message accepted");
    }

    private Future<List<Message>> setUpMessagingConsumer(String type, AtomicInteger receivedMessagesCounter) {
        return businessApplicationClient.recvMessages(type + "/" + tenantId(), msg ->{
            if(msg.getBody() instanceof Data) {
                Binary value = ((Data) msg.getBody()).getValue();
                JsonObject json = new JsonObject(Buffer.buffer(value.getArray()));
                if(json.containsKey("i")) {
                    receivedMessagesCounter.incrementAndGet();
                    return json.getBoolean("end");
                }
            }
            return false;
        });
    }

}

