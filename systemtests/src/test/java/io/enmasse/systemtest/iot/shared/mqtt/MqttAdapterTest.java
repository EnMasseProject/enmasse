/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.shared.mqtt;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTShared;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.vertx.core.json.JsonObject;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.SHARED_IOT;

/**
 * Testing MQTT message transmission.
 * <br>
 * <strong>Note:</strong> we do not test single telemetry with QoS 0 here, as we don't receive any feedback if the message
 * was accepted or not. So we couldn't re-try and could only assume that a message loss of 100% would be acceptable,
 * which doesn't test much. For bigger batch sizes we can test with an acceptable message loss rate of e.g. 10%.
 */
class MqttAdapterTest extends TestBase implements ITestIoTShared {

    private static final Logger log = CustomLogger.getLogger();
    private final String deviceId = TestUtils.randomCharacters(23 /* max client ID length */);
    private final String deviceAuthId = UUID.randomUUID().toString();
    private final String devicePassword = UUID.randomUUID().toString();
    private final String businessApplicationUsername = UUID.randomUUID().toString();
    private final String businessApplicationPassword = UUID.randomUUID().toString();
    private Endpoint mqttAdapterEndpoint;
    private Endpoint deviceRegistryEndpoint;
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;
    private IMqttAsyncClient adapterClient;
    private AmqpClient businessApplicationClient;

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
        registryClient.registerDevice(sharedIoTResourceManager.getTenantID(), deviceId);
        credentialsClient.addCredentials(sharedIoTResourceManager.getTenantID(), deviceId, deviceAuthId, devicePassword);

        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setAutomaticReconnect(true);
        mqttOptions.setConnectionTimeout(60);
        // do not reject due to "inflight" messages. Note: this will allocate an array of that size.
        mqttOptions.setMaxInflight(16 * 1024);
        mqttOptions.setHttpsHostnameVerificationEnabled(false);
        adapterClient = new MqttClientFactory(null, new UserCredentials(deviceAuthId + "@"
                + sharedIoTResourceManager.getTenantID(), devicePassword))
                .build()
                .clientId(deviceId)
                .endpoint(mqttAdapterEndpoint)
                .mqttConnectionOptions(mqttOptions)
                .createAsync();

        TestUtils.waitUntilCondition("Successfully connect to mqtt adapter", phase -> {
            try {
                adapterClient.connect().waitForCompletion(10_000);
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
                                .withAddresses(IOT_ADDRESS_TELEMETRY + "/" + sharedIoTResourceManager.getTenantID(),
                                        IOT_ADDRESS_TELEMETRY + "/" + sharedIoTResourceManager.getTenantID() + "/*",
                                        IOT_ADDRESS_EVENT + "/" + sharedIoTResourceManager.getTenantID(),
                                        IOT_ADDRESS_EVENT + "/" + sharedIoTResourceManager.getTenantID() + "/*")
                                .withOperations(Operation.recv)
                                .build()))
                .endSpec()
                .done();

        AddressSpace addressSpace = resourcesManager.getAddressSpace(iotProjectNamespace, getSharedIoTProject().getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());

        IsolatedResourcesManager.getInstance().createOrUpdateUser(addressSpace, businessApplicationUser);

        businessApplicationClient = getAmqpClientFactory().createQueueClient(addressSpace);
        businessApplicationClient.getConnectOptions()
                .setUsername(businessApplicationUsername)
                .setPassword(businessApplicationPassword);

    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectMqttAdapterQdrProxyState();
        }
        credentialsClient.deleteAllCredentials(sharedIoTResourceManager.getTenantID(), deviceId);
        registryClient.deleteDeviceRegistration(sharedIoTResourceManager.getTenantID(), deviceId);
        registryClient.getDeviceRegistration(sharedIoTResourceManager.getTenantID(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        if (adapterClient.isConnected()) {
            adapterClient.disconnect();
            adapterClient.close();
        }

        IsolatedResourcesManager.getInstance().removeUser(getSharedAddressSpace(), businessApplicationUsername);
    }

    @AfterEach
    void closeClient() throws Exception {
        // close in a dedicated method to ensure it gets called in any case
        if (businessApplicationClient != null) {
            businessApplicationClient.close();
            businessApplicationClient = null;
        }
    }

    /**
     * Single telemetry message with attached consumer. QoS 1.
     * <br>
     * Sending with QoS 1 is ok.
     */
    @Test
    @Tag(ACCEPTANCE)
    void testTelemetrySingleQos1() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.TELEMETRY)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, sharedIoTResourceManager.getTenantID()))
                .sender(this::sendQos1)
                .amount(1)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }

    /**
     * Single event message with non-attached consumer.
     * <br>
     * This is the normal use case.
     */
    @Test
    void testEventSingle() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.EVENT)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, sharedIoTResourceManager.getTenantID()))
                .sender(this::sendQos1)
                .amount(1)
                .consume(MessageSendTester.Consume.AFTER)
                .execute();
    }

    /**
     * Batch of telemetry messages with attached consumer. QoS 0.
     * <br>
     * Batched version of the normal use case. We do accept message loss of 10% here.
     */
    @Test
    void testTelemetryBatch50Qos0() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.TELEMETRY)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, sharedIoTResourceManager.getTenantID()))
                .sender(this::sendQos0)
                .amount(50)
                .consume(MessageSendTester.Consume.BEFORE)
                .acceptableMessageLoss(5) // allow for 10%
                .execute();
    }

    /**
     * Batch of telemetry messages with attached consumer. QoS 1.
     * <br>
     * Compared to QoS 0, we do not accept message loss here.
     */
    @Test
    void testTelemetryBatch50Qos1() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.TELEMETRY)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, sharedIoTResourceManager.getTenantID()))
                .sender(this::sendQos1)
                .amount(50)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }

    /**
     * Batch of event messages with non-attached consumer.
     * <br>
     * This sends messages without an attached consumer. The broker is expected
     * to take care of that. Still we expect to receive the messages later.
     */
    @Test
    void testEventBatch5After() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.EVENT)
                .delay(Duration.ofMillis(100))
                .additionalSendTimeout(Duration.ofSeconds(2))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, sharedIoTResourceManager.getTenantID()))
                .sender(this::sendQos1)
                .amount(5)
                .consume(MessageSendTester.Consume.AFTER)
                .execute();
    }

    /**
     * Batch of event messages with attached consumer.
     * <br>
     * This is the normal use case.
     */
    @Test
    void testEventBatch5Before() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.EVENT)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, sharedIoTResourceManager.getTenantID()))
                .sender(this::sendQos1)
                .amount(5)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }

    private boolean sendQos0(MessageSendTester.Type type, JsonObject json, Duration timeout) {
        return send(0, type, json, timeout);
    }

    private boolean sendQos1(MessageSendTester.Type type, JsonObject json, Duration timeout) {
        return send(1, type, json, timeout);
    }

    private boolean send(int qos, final MessageSendTester.Type type, final JsonObject json, final Duration timeout) {
        final MqttMessage message = new MqttMessage(json.toBuffer().getBytes());
        message.setQos(qos);
        try {
            final IMqttDeliveryToken token = adapterClient.publish(type.type().address(), message);
            if (qos <= 0) {
                return true; // we know nothing with QoS 0
            }
            token.waitForCompletion(timeout.toMillis());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
