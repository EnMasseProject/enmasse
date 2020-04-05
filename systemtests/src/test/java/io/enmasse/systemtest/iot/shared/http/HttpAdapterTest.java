/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.shared.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.annotations.DefaultIoT;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.IoTConstants;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;

@Tag(TestTag.SHARED_IOT)
@DefaultIoT
class HttpAdapterTest extends TestBase {

    @SuppressWarnings("unused")
    private static final Logger log = CustomLogger.getLogger();
    private final String deviceId = UUID.randomUUID().toString();
    private final String deviceAuthId = UUID.randomUUID().toString();
    private final String devicePassword = UUID.randomUUID().toString();
    private final String businessApplicationUsername = UUID.randomUUID().toString();
    private final String businessApplicationPassword = UUID.randomUUID().toString();
    private Endpoint deviceRegistryEndpoint;
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;
    private AmqpClient businessApplicationClient;
    private HttpAdapterClient adapterClient;

    @BeforeEach
    void initEnv() throws Exception {
        deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        registryClient = new DeviceRegistryClient(deviceRegistryEndpoint);
        credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint);
        registryClient.registerDevice(resourceManager.getDefaultTenantId(), deviceId);
        credentialsClient.addCredentials(resourceManager.getDefaultTenantId(), deviceId, deviceAuthId, devicePassword);

        User businessApplicationUser = UserUtils.createUserResource(new UserCredentials(businessApplicationUsername, businessApplicationPassword))
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withAddresses(
                                        IoTConstants.IOT_ADDRESS_TELEMETRY + "/" + resourceManager.getDefaultTenantId(),
                                        IoTConstants.IOT_ADDRESS_TELEMETRY + "/" + resourceManager.getDefaultTenantId() + "/*",
                                        IoTConstants.IOT_ADDRESS_EVENT + "/" + resourceManager.getDefaultTenantId(),
                                        IoTConstants.IOT_ADDRESS_EVENT + "/" + resourceManager.getDefaultTenantId() + "/*")
                                .withOperations(Operation.recv)
                                .build()))
                .endSpec()
                .done();

        AddressSpace addressSpace = resourceManager.getDefaultIotAddressSpace();

        resourceManager.createOrUpdateUser(addressSpace, businessApplicationUser);

        businessApplicationClient = resourceManager.getAmqpClientFactory().createQueueClient(addressSpace);
        businessApplicationClient.getConnectOptions()
                .setUsername(businessApplicationUsername)
                .setPassword(businessApplicationPassword);

        Endpoint httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        adapterClient = new HttpAdapterClient(httpAdapterEndpoint, deviceAuthId, resourceManager.getDefaultTenantId(), devicePassword);

    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (credentialsClient != null) {
            credentialsClient.deleteAllCredentials(resourceManager.getDefaultTenantId(), deviceId);
        }
        if (registryClient != null) {
            registryClient.deleteDeviceRegistration(resourceManager.getDefaultTenantId(), deviceId);
            registryClient.getDeviceRegistration(resourceManager.getDefaultTenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        }
        if (adapterClient != null) {
            adapterClient.close();
        }
        var addressSpace = resourceManager.getDefaultIotAddressSpace();
        if(addressSpace != null) {
            resourceManager.removeUser(addressSpace, businessApplicationUsername);
        }
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
     * Single telemetry message with attached consumer.
     */
    @Test
    @Tag(ACCEPTANCE)
    void testTelemetrySingle() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.TELEMETRY)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, resourceManager.getDefaultTenantId()))
                .sender(adapterClient::send)
                .amount(1)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }

    /**
     * Test a single event message.
     * <br>
     * Send a single message, no consumer attached. The message gets delivered
     * when the consumer attaches.
     */
    @Test
    public void testEventSingle() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.EVENT)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, resourceManager.getDefaultTenantId()))
                .sender(adapterClient::send)
                .amount(1)
                .consume(MessageSendTester.Consume.AFTER)
                .execute();
    }

    /**
     * Test a batch of telemetry messages, consumer is started before sending.
     * <br>
     * This is the normal telemetry case.
     */
    @Test
    void testTelemetryBatch50() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.TELEMETRY)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, resourceManager.getDefaultTenantId()))
                .sender(adapterClient::send)
                .amount(50)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }

    /**
     * Test a batch of events, having no consumer attached.
     * <br>
     * As events get buffered by the broker, there is no requirement to start
     * a consumer before sending the messages. However when the consumer is
     * attached, it should receive those messages.
     */
    @Test
    void testEventBatch5After() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.EVENT)
                .delay(Duration.ofMillis(100))
                .additionalSendTimeout(Duration.ofSeconds(10))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, resourceManager.getDefaultTenantId()))
                .sender(adapterClient::send)
                .amount(5)
                .consume(MessageSendTester.Consume.AFTER)
                .execute();
    }

    /**
     * Test a batch of events, starting the consumer before sending.
     * <br>
     * This is the default use case with events, and should simply work
     * as with telemetry.
     */
    @Test
    void testEventBatch5Before() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.EVENT)
                .delay(Duration.ZERO)
                .additionalSendTimeout(Duration.ofSeconds(10))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, resourceManager.getDefaultTenantId()))
                .sender(adapterClient::send)
                .amount(5)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }
}

