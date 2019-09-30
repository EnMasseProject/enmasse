/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTIsolated;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.IoTProjectTestContext;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.enmasse.user.model.v1.UserBuilder;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.bases.iot.ITestIoTBase.IOT_ADDRESS_EVENT;
import static io.enmasse.systemtest.bases.iot.ITestIoTBase.IOT_ADDRESS_TELEMETRY;
import static io.enmasse.systemtest.utils.IoTUtils.createIoTConfig;
import static io.enmasse.systemtest.utils.IoTUtils.createIoTProject;

@Tag(TestTag.SMOKE)
class MultipleProjectsTest extends TestBase implements ITestIoTIsolated {
    private static Logger log = CustomLogger.getLogger();
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;

    private int numberOfProjects = 2;
    private List<IoTProjectTestContext> projects = new ArrayList<>();

    @BeforeEach
    void initEnv() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();
        IoTConfig iotConfig = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withNewServices()
                .withNewDeviceRegistry()
                .withNewFile()
                .withNumberOfDevicesPerTenant(10_0000)
                .endFile()
                .endDeviceRegistry()
                .endServices()
                .withNewAdapters()
                .withNewMqtt()
                .withNewEndpoint()
                .withNewKeyCertificateStrategy()
                .withCertificate(ByteBuffer.wrap(certBundle.getCert().getBytes()))
                .withKey(ByteBuffer.wrap(certBundle.getKey().getBytes()))
                .endKeyCertificateStrategy()
                .endEndpoint()
                .endMqtt()
                .endAdapters()
                .endSpec()
                .build();
        isolatedIoTManager.createIoTConfig(iotConfig);

        Endpoint deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        registryClient = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint);

        for (int i = 1; i <= numberOfProjects; i++) {
            String projectName = String.format("project-%s", i);

            if (!kubernetes.namespaceExists(projectName)) {
                kubernetes.createNamespace(projectName);
            }
            IoTProject project = IoTUtils.getBasicIoTProjectObject(projectName, projectName,
                    projectName, getDefaultAddressSpacePlan());
            isolatedIoTManager.createIoTProject(project);
            IoTProjectTestContext ctx = new IoTProjectTestContext(projectName, project);

            configureDeviceSide(ctx);

            configureAmqpSide(ctx);

            projects.add(ctx);
        }
    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        isolatedIoTManager.getIoTProjects().forEach(project -> LOGGER.warn("PROJECTS {}", project));
        isolatedIoTManager.getIoTConfigs().forEach(config -> LOGGER.warn("CONFIG {}", config));

        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectHttpAdapterQdrProxyState();
        }

        for (IoTProjectTestContext ctx : projects) {
            cleanDeviceSide(ctx);
            cleanAmqpSide(ctx);
        }

        SystemtestsKubernetesApps.deleteInfinispanServer(kubernetes.getInfraNamespace());
        isolatedIoTManager.getIoTProjects().forEach(project -> LOGGER.warn("PROJECTS {}", project));
        isolatedIoTManager.getIoTConfigs().forEach(config -> LOGGER.warn("CONFIG {}", config));
    }

    @Test
    void testMultipleProjects() throws Exception {

        for (final IoTProjectTestContext ctx : projects) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(ctx.getAmqpClient(), IoTUtils.getTenantID(ctx.getProject())))
                    .sender(ctx.getHttpAdapterClient()::send)
                    .amount(50)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();

            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ofMillis(100))
                    .consumerFactory(ConsumerFactory.of(ctx.getAmqpClient(), IoTUtils.getTenantID(ctx.getProject())))
                    .sender(ctx.getHttpAdapterClient()::send)
                    .amount(5)
                    .consume(MessageSendTester.Consume.AFTER)
                    .execute();
        }

    }

    private void configureAmqpSide(IoTProjectTestContext ctx) throws Exception {
        AddressSpace addressSpace = isolatedIoTManager.getAddressSpace(ctx.getNamespace(),
                ctx.getProject().getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());
        User amqpUser = configureAmqpUser(ctx.getProject(), addressSpace);
        ctx.setAmqpUser(amqpUser);
        AmqpClient amqpClient = configureAmqpClient(addressSpace, amqpUser);
        ctx.setAmqpClient(amqpClient);
    }

    private User configureAmqpUser(IoTProject project, AddressSpace addressSpace) {
        String tenant = IoTUtils.getTenantID(project);

        User amqpUser = new UserBuilder()

                .withNewMetadata()
                .withName(String.format("%s.%s", addressSpace.getMetadata().getName(), project.getMetadata().getName()))
                .endMetadata()

                .withNewSpec()
                .withUsername(UUID.randomUUID().toString())
                .withNewAuthentication()
                .withPassword(Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)))
                .withType(UserAuthenticationType.password)
                .endAuthentication()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses(IOT_ADDRESS_TELEMETRY + "/" + tenant,
                                IOT_ADDRESS_TELEMETRY + "/" + tenant + "/*",
                                IOT_ADDRESS_EVENT + "/" + tenant,
                                IOT_ADDRESS_EVENT + "/" + tenant + "/*")
                        .withOperations(Operation.recv)
                        .build()))
                .endSpec()
                .build();
        kubernetes.getUserClient(project.getMetadata().getNamespace()).create(amqpUser);

        return amqpUser;
    }

    private AmqpClient configureAmqpClient(AddressSpace addressSpace, User user) throws Exception {
        LOGGER.warn("Amqp factory: " + getAmqpClientFactory());
        AmqpClient amqpClient = getAmqpClientFactory().createQueueClient(addressSpace);
        amqpClient.getConnectOptions()
                .setUsername(user.getSpec().getUsername())
                .setPassword(new String(Base64.getDecoder().decode(user.getSpec().getAuthentication().getPassword())));
        return amqpClient;
    }

    private void cleanAmqpSide(IoTProjectTestContext ctx) throws Exception {
        ctx.getAmqpClient().close();
        var userClient = kubernetes.getUserClient(ctx.getNamespace());
        userClient.withName(ctx.getAmqpUser().getMetadata().getName()).cascading(true).delete();
    }

    private void configureDeviceSide(IoTProjectTestContext ctx) throws Exception {
        String tenant = IoTUtils.getTenantID(ctx.getProject());
        ctx.setDeviceId(UUID.randomUUID().toString());
        ctx.setDeviceAuthId(UUID.randomUUID().toString());
        ctx.setDevicePassword(UUID.randomUUID().toString());
        registryClient.registerDevice(tenant, ctx.getDeviceId());
        credentialsClient.addCredentials(tenant, ctx.getDeviceId(), ctx.getDeviceAuthId(), ctx.getDevicePassword());
        Endpoint httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        ctx.setHttpAdapterClient(new HttpAdapterClient(kubernetes, httpAdapterEndpoint, ctx.getDeviceAuthId(), tenant, ctx.getDevicePassword()));
        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setAutomaticReconnect(true);
        mqttOptions.setConnectionTimeout(60);
        mqttOptions.setHttpsHostnameVerificationEnabled(false);
        IMqttClient mqttAdapterClient = new MqttClientFactory(null, new UserCredentials(ctx.getDeviceAuthId() + "@" + tenant, ctx.getDevicePassword()))
                .build()
                .clientId(ctx.getDeviceId())
                .endpoint(kubernetes.getExternalEndpoint("iot-mqtt-adapter"))
                .mqttConnectionOptions(mqttOptions)
                .create();
        TestUtils.waitUntilCondition("Successfully connect to mqtt adapter", phase -> {
            try {
                mqttAdapterClient.connect();
                return true;
            } catch (MqttException mqttException) {
                if (phase == WaitPhase.LAST_TRY) {
                    log.error("Error waiting to connect mqtt adapter", mqttException);
                }
                return false;
            }
        }, new TimeoutBudget(1, TimeUnit.MINUTES));
        log.info("Connection to mqtt adapter succeeded");
        ctx.setMqttAdapterClient(mqttAdapterClient);
    }

    private void cleanDeviceSide(IoTProjectTestContext ctx) throws Exception {
        String tenant = IoTUtils.getTenantID(ctx.getProject());
        String deviceId = ctx.getDeviceId();
        credentialsClient.deleteAllCredentials(tenant, deviceId);
        registryClient.deleteDeviceRegistration(tenant, deviceId);
        registryClient.getDeviceRegistration(tenant, deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        ctx.getHttpAdapterClient().close();
        if (ctx.getMqttAdapterClient().isConnected()) {
            ctx.getMqttAdapterClient().disconnect();
            ctx.getMqttAdapterClient().close();
        }
    }

}
