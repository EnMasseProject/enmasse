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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
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

    private static final String DUMMY_DEVICE_ID = "1234";
    private Endpoint mqttAdapterEndpoint;
    private Endpoint deviceRegistryEndpoint;
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;
    private IMqttClient adapterClient;
    private AmqpClient businessApplicationClient;

    private String deviceAuthId = "sensor1234";
    private String devicePassword = "devicePwd1234";
    private String businessApplicationUsername = "businessuser";
    private String businessApplicationPassword = "businesspwd";

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
        try {
            registryClient.registerDevice(tenantId(), DUMMY_DEVICE_ID);
            credentialsClient.addCredentials(tenantId(), DUMMY_DEVICE_ID, deviceAuthId, devicePassword);
        }catch(Exception e) {
            e.printStackTrace();
        }

        adapterClient = new MqttClientFactory(kubernetes, environment, null, new UserCredentials(deviceAuthId + "@" + tenantId(), devicePassword))
                .build()
                .clientId(DUMMY_DEVICE_ID)
                .endpoint(mqttAdapterEndpoint)
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
        credentialsClient.deleteAllCredentials(tenantId(), DUMMY_DEVICE_ID);
        credentialsClient.getCredentials(tenantId(), DUMMY_DEVICE_ID, HttpURLConnection.HTTP_NOT_FOUND);
        registryClient.deleteDeviceRegistration(tenantId(), DUMMY_DEVICE_ID);
        registryClient.getDeviceRegistration(tenantId(), DUMMY_DEVICE_ID, HttpURLConnection.HTTP_NOT_FOUND);
        adapterClient.disconnect();
        adapterClient.close();
        businessApplicationClient.close();
    }

    @Test
    public void basicTelemetryTest() throws Exception {

        log.info("Connecting amqp consumer");
        AtomicInteger receivedMessagesCounter = new AtomicInteger(0);
        Future<List<Message>> futureReceivedMessages = businessApplicationClient.recvMessages(IOT_ADDRESS_TELEMETRY+ "/" + tenantId(), msg ->{
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

        log.info("Sending telemetry messages");
        for ( int i = 0; i < 50; i++ ) {
            JsonObject json = new JsonObject();
            json.put("i", i);
            json.put("end", i == 49);
            MqttMessage message = new MqttMessage(json.toBuffer().getBytes());
            message.setQos(0);
            adapterClient.publish(IOT_ADDRESS_TELEMETRY, message);
        }

        log.info("Waiting to receive telemetry data in business application");
        futureReceivedMessages.get(15, TimeUnit.SECONDS);
        assertEquals(50, receivedMessagesCounter.get());

    }

    protected String tenantId() {
        return String.format("%s.%s", sharedProject.getMetadata().getNamespace(), sharedProject.getMetadata().getName());
    }

}
