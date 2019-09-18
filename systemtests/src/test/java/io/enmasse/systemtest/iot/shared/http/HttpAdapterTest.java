/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.shared.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.IoTTestBaseWithShared;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.CommonResourcesManager;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
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

import static io.enmasse.systemtest.TestTag.SHARED_IOT;

@Tag(SHARED_IOT)
public class HttpAdapterTest extends IoTTestBaseWithShared implements ITestIsolatedStandard {

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
        registryClient = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint);
        registryClient.registerDevice(tenantId(), deviceId);
        credentialsClient.addCredentials(tenantId(), deviceId, deviceAuthId, devicePassword);

        User businessApplicationUser = UserUtils.createUserResource(new UserCredentials(businessApplicationUsername, businessApplicationPassword))
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withAddresses(
                                        IOT_ADDRESS_TELEMETRY + "/" + tenantId(),
                                        IOT_ADDRESS_TELEMETRY + "/" + tenantId() + "/*",
                                        IOT_ADDRESS_EVENT + "/" + tenantId(),
                                        IOT_ADDRESS_EVENT + "/" + tenantId() + "/*")
                                .withOperations(Operation.recv)
                                .build()))
                .endSpec()
                .done();

        AddressSpace addressSpace = resourcesManager.getAddressSpace(iotProjectNamespace, sharedProject.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());

        CommonResourcesManager.getInstance().createOrUpdateUser(addressSpace, businessApplicationUser);

        businessApplicationClient = getAmqpClientFactory().createQueueClient(addressSpace);
        businessApplicationClient.getConnectOptions()
                .setUsername(businessApplicationUsername)
                .setPassword(businessApplicationPassword);

        Endpoint httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        adapterClient = new HttpAdapterClient(kubernetes, httpAdapterEndpoint, deviceAuthId, tenantId(), devicePassword);

    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectHttpAdapterQdrProxyState();
        }
        if (credentialsClient != null) {
            credentialsClient.deleteAllCredentials(tenantId(), deviceId);
        }
        if (registryClient != null) {
            registryClient.deleteDeviceRegistration(tenantId(), deviceId);
            registryClient.getDeviceRegistration(tenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        }
        if (adapterClient != null) {
            adapterClient.close();
        }
        CommonResourcesManager.getInstance().removeUser(getAddressSpace(), businessApplicationUsername);
    }

    @AfterEach
    public void closeClient() throws Exception {
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
    public void testTelemetrySingle() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.TELEMETRY)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, tenantId()))
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
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, tenantId()))
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
    public void testTelemetryBatch50() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.TELEMETRY)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, tenantId()))
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
    public void testEventBatch5After() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.EVENT)
                .delay(Duration.ofMillis(100))
                .additionalSendTimeout(Duration.ofSeconds(10))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, tenantId()))
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
    public void testEventBatch5Before() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.EVENT)
                .delay(Duration.ZERO)
                .additionalSendTimeout(Duration.ofSeconds(10))
                .consumerFactory(ConsumerFactory.of(businessApplicationClient, tenantId()))
                .sender(adapterClient::send)
                .amount(5)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }

}

