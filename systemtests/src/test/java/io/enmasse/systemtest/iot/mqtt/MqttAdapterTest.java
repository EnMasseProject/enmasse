/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.mqtt;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.ReceiverStatus;
import io.enmasse.systemtest.bases.IoTTestBaseWithShared;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@Tag(sharedIot)
public class MqttAdapterTest extends IoTTestBaseWithShared {

    private Logger log = CustomLogger.getLogger();

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
        adapterClient = new MqttClientFactory(kubernetes, environment, null, new UserCredentials(deviceAuthId + "@" + tenantId(), devicePassword))
                .build()
                .clientId(deviceId)
                .endpoint(mqttAdapterEndpoint)
                .mqttConnectionOptions(mqttOptions)
                .create();
        adapterClient.connect();

        log.info("Connection to mqtt adapter succeeded");

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

    }

    @AfterEach
    void cleanEnv() throws Exception {
        credentialsClient.deleteAllCredentials(tenantId(), deviceId);
        credentialsClient.getCredentials(tenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        registryClient.deleteDeviceRegistration(tenantId(), deviceId);
        registryClient.getDeviceRegistration(tenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        adapterClient.disconnect();
        adapterClient.close();
        businessApplicationClient.close();
        removeUser(getAddressSpace(), businessApplicationUsername);
    }

    @Test
    public void basicTelemetryTest() throws Exception {

        var timeout = new TimeoutBudget(2, TimeUnit.MINUTES);
        businessApplicationClient.recvMessages(IOT_ADDRESS_TELEMETRY + "/" + tenantId(), msg -> {
            log.info("First telemetry message accepted");
            timeout.reset(0, TimeUnit.MILLISECONDS);
            return true;
        });

        var warmUpMessage = new JsonObject(Map.of("a", "b"));
        while(!timeout.timeoutExpired()) {
            MqttMessage message = new MqttMessage(warmUpMessage.toBuffer().getBytes());
            message.setQos(0);
            log.info("Sending telemetry data");
            adapterClient.publish(IOT_ADDRESS_TELEMETRY, message);
            Thread.sleep(5000);
        }

    }

    @Test
    @Disabled
    public void batchTelemetryTest() throws Exception {
        log.info("Connecting amqp consumer");
        AtomicInteger receivedMessagesCounter = new AtomicInteger(0);
        Future<List<Message>> futureReceivedMessages = businessApplicationClient.recvMessages(IOT_ADDRESS_TELEMETRY + "/" + tenantId(), msg ->{
            if(msg.getBody() instanceof Data) {
                Binary value = ((Data) msg.getBody()).getValue();
                JsonObject json = new JsonObject(Buffer.buffer(value.getArray()));
                if(json.containsKey("i")) {
                    log.info("Telemetry message received");
                    receivedMessagesCounter.incrementAndGet();
                    return json.getBoolean("end");
                }
            }
            return false;
        });

        int messagesToSend = 50;
        log.info("Sending telemetry messages");
        for ( int i = 0; i < messagesToSend; i++ ) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            json.put("end", i == (messagesToSend-1));
            MqttMessage message = new MqttMessage(json.toBuffer().getBytes());
            message.setQos(0);
            adapterClient.publish(IOT_ADDRESS_TELEMETRY, message);
        }

        log.info("Waiting to receive telemetry data in business application");
        futureReceivedMessages.get(60, TimeUnit.SECONDS);
        assertEquals(messagesToSend, receivedMessagesCounter.get());
    }

    @Test
    public void basicEventTest() throws Exception {

        int messagesToSend = 5;
        log.info("Sending events");
        for ( int i = 0; i < messagesToSend; i++ ) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            json.put("end", i == (messagesToSend-1));
            MqttMessage message = new MqttMessage(json.toBuffer().getBytes());
            message.setQos(1);
            adapterClient.publish(IOT_ADDRESS_EVENT, message);
        }

        log.info("Consuming events in business application");
        ReceiverStatus status = businessApplicationClient.recvMessagesWithStatus(IOT_ADDRESS_EVENT + "/" + tenantId(), 5);

        try {
            log.info("Waiting to receive events");
            List<Message> messages = status.getResult().get(60, TimeUnit.SECONDS);
            assertEquals(messagesToSend, messages.size());
        }catch(TimeoutException e) {
            log.error("Timeout receiving events, messages received: {} error:", status.getNumReceived(), e);
            throw e;
        }

    }

}
