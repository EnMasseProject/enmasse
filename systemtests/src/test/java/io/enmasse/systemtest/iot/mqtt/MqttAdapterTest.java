/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.mqtt;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.IoTTestBaseWithShared;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(sharedIot)
public class MqttAdapterTest extends IoTTestBaseWithShared {

    private static final Logger log = CustomLogger.getLogger();

    private Endpoint mqttAdapterEndpoint;
    private Endpoint deviceRegistryEndpoint;
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;
    private IMqttClient adapterClient;
    private AmqpClient businessApplicationClient;

    private final String deviceId = UUID.randomUUID().toString();
    private final String deviceAuthId = UUID.randomUUID().toString();
    private final String devicePassword = UUID.randomUUID().toString();
    private final String businessApplicationUsername = UUID.randomUUID().toString();
    private final String businessApplicationPassword = UUID.randomUUID().toString();

    @BeforeEach
    void initEnv() throws Exception {
        if (deviceRegistryEndpoint == null) {
            deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        }
        if (mqttAdapterEndpoint == null) {
            mqttAdapterEndpoint = kubernetes.getExternalEndpoint("iot-mqtt-adapter");
        }
        if (registryClient == null) {
            registryClient = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        }
        if (credentialsClient == null) {
            credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint);
        }
        registryClient.registerDevice(tenantId(), deviceId);
        credentialsClient.addCredentials(tenantId(), deviceId, deviceAuthId, devicePassword);

        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setAutomaticReconnect(true);
        mqttOptions.setConnectionTimeout(60);
        adapterClient = new MqttClientFactory(null, new UserCredentials(deviceAuthId + "@" + tenantId(), devicePassword))
                .build()
                .clientId(deviceId)
                .endpoint(mqttAdapterEndpoint)
                .mqttConnectionOptions(mqttOptions)
                .create();

        TestUtils.waitUntilCondition("Successfully connect to mqtt adapter", phase -> {
            try {
                adapterClient.connect();
                return true;
            } catch (MqttException mqttException) {
                if (phase == WaitPhase.LAST_TRY) {
                    log.error("Error waiting to connect mqtt adapter", mqttException);
                }
                return false;
            }
        }, new TimeoutBudget(1, TimeUnit.MINUTES));

        log.info("Connection to mqtt adapter succeeded");

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

    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectMqttAdapterQdrProxyState();
        }
        credentialsClient.deleteAllCredentials(tenantId(), deviceId);
        credentialsClient.getCredentials(tenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        registryClient.deleteDeviceRegistration(tenantId(), deviceId);
        registryClient.getDeviceRegistration(tenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        if (adapterClient.isConnected()) {
            adapterClient.disconnect();
            adapterClient.close();
        }
        businessApplicationClient.close();
        removeUser(getAddressSpace(), businessApplicationUsername);
    }

    @Test
    public void basicTelemetryTest() throws Exception {
        consumeOneMessageTest(IOT_ADDRESS_TELEMETRY, businessApplicationClient, tenantId(), adapterClient);
    }

    @Test
    @Disabled
    public void batchTelemetryTest() throws Exception {
        simpleMqttTelemetryTest(businessApplicationClient, tenantId(), adapterClient);
    }

    public static void simpleMqttTelemetryTest(AmqpClient amqpClient, String tenantId, IMqttClient mqttClient) throws Exception {
        int messagesToSend = 50;

        log.info("Connecting amqp consumer");
        AtomicInteger receivedMessagesCounter = new AtomicInteger(0);
        Future<List<Message>> futureReceivedMessages = setUpMessagingConsumer(amqpClient, IOT_ADDRESS_TELEMETRY, tenantId, receivedMessagesCounter, messagesToSend);

        log.info("doing first telemetry attemp");
        consumeOneMessageTest(IOT_ADDRESS_TELEMETRY, amqpClient, tenantId, mqttClient);

        log.info("Sending telemetry messages");
        for (int i = 0; i < messagesToSend; i++) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            MqttMessage message = new MqttMessage(json.toBuffer().getBytes());
            message.setQos(0);
            mqttClient.publish(IOT_ADDRESS_TELEMETRY, message);
        }

        try {
            log.info("Waiting to receive telemetry data in business application");
            futureReceivedMessages.get(120, TimeUnit.SECONDS);
            assertEquals(messagesToSend, receivedMessagesCounter.get());
            log.info("Telemetry successfully consumed");
        } catch (TimeoutException e) {
            log.error("Timeout receiving telemetry, messages received: {} error:", receivedMessagesCounter.get(), e);
            throw e;
        }
    }

    @Test
    public void basicEventTest1() throws Exception {
        consumeOneEventMessageTest(businessApplicationClient, tenantId(), adapterClient);
    }

    private static void consumeOneEventMessageTest(AmqpClient amqpClient, String tenantId, IMqttClient mqttClient) throws Exception {
        int messagesToSend = 5;
        log.info("Sending events");
        for (int i = 0; i < messagesToSend; i++) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            MqttMessage message = new MqttMessage(json.toBuffer().getBytes());
            message.setQos(0);
            mqttClient.publish(IOT_ADDRESS_EVENT, message);
        }

        log.info("Consuming one event in business application");
        CountDownLatch latch = new CountDownLatch(1);
        amqpClient.recvMessages(IOT_ADDRESS_EVENT + "/" + tenantId, msg -> {
            latch.countDown();
            return true;
        });

        latch.await(30, TimeUnit.SECONDS);
        log.info("Event successfully consumed");
    }

    @Test
    @Disabled
    public void basicEventTest2() throws Exception {
        consumeOneMessageTest(IOT_ADDRESS_EVENT, businessApplicationClient, tenantId(), adapterClient);
    }

    @Test
    @Disabled
    public void batchEventTest() throws Exception {
        simpleMqttEventTest(businessApplicationClient, tenantId(), adapterClient);
    }

    public static void simpleMqttEventTest(AmqpClient amqpClient, String tenantId, IMqttClient mqttClient) throws Exception{

        log.info("doing first event attemp");
        consumeOneEventMessageTest(amqpClient, tenantId, mqttClient);

        int eventsToSend = 5;
        log.info("Sending events");
        for (int i = 0; i < eventsToSend; i++) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            MqttMessage message = new MqttMessage(json.toBuffer().getBytes());
            message.setQos(0);
            mqttClient.publish(IOT_ADDRESS_EVENT, message);
        }

        log.info("Consuming events in business application");
        AtomicInteger receivedMessagesCounter = new AtomicInteger(0);
        Future<List<Message>> status = setUpMessagingConsumer(amqpClient, IOT_ADDRESS_EVENT, tenantId, receivedMessagesCounter, eventsToSend);

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

    private static Future<List<Message>> setUpMessagingConsumer(AmqpClient amqpClient, String type, String tenantId, AtomicInteger receivedMessagesCounter, int expectedMessages) {
        return amqpClient.recvMessages(type + "/" + tenantId, msg ->{
            if(msg.getBody() instanceof Data) {
                Binary value = ((Data) msg.getBody()).getValue();
                JsonObject json = new JsonObject(Buffer.buffer(value.getArray()));
                if (json.containsKey("i")) {
                    return receivedMessagesCounter.incrementAndGet() == expectedMessages;
                }
            }
            return false;
        });
    }

    private static void consumeOneMessageTest(String address, AmqpClient amqpClient, String tenantId, IMqttClient mqttClient) throws Exception {
        AtomicBoolean success = new AtomicBoolean(false);
        var timeout = new TimeoutBudget(2, TimeUnit.MINUTES);
        amqpClient.recvMessages(address + "/" + tenantId, msg -> {
            log.info("First {} message accepted", address);
            success.set(true);
            timeout.reset(0, TimeUnit.MILLISECONDS);
            return true;
        });

        var warmUpMessage = new JsonObject(Map.of("a", "b"));
        while (!timeout.timeoutExpired()) {
            MqttMessage message = new MqttMessage(warmUpMessage.toBuffer().getBytes());
            message.setQos(0);
            log.info("Sending {} data", address);
            mqttClient.publish(address, message);
            Thread.sleep(5000);
        }
        if(!success.get()) {
            String errorMsg = String.format("Failed waiting for first accepted %s message", address);
            log.error(errorMsg);
            Assertions.fail(errorMsg);
        }
    }

}
