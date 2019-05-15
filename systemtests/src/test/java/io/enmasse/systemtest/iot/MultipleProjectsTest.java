/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.CertBundle;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.SystemtestsKubernetesApps;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.WaitPhase;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.IoTTestBase;
import io.enmasse.systemtest.iot.http.HttpAdapterTest;
import io.enmasse.systemtest.iot.mqtt.MqttAdapterTest;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.enmasse.user.model.v1.UserBuilder;

@Tag(TestTag.sharedIot)
@Tag(TestTag.smoke)
public class MultipleProjectsTest extends IoTTestBase implements ITestBaseStandard {

    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;

    private int numberOfProjects = 2;
    private List<IoTProjectTestContext> projects = new ArrayList<>();

    @BeforeEach
    void initEnv() throws Exception {
        Endpoint infinispanEndpoint = SystemtestsKubernetesApps.deployInfinispanServer(kubernetes.getInfraNamespace());
        CertBundle certBundle = CertificateUtils.createCertBundle();
        IoTConfig iotConfig = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withNewServices()
                .withNewDeviceRegistry()
                .withNewInfinispan()
                .withInfinispanServerAddress(infinispanEndpoint.toString())
                .endInfinispan()
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
        createIoTConfig(iotConfig);

        Endpoint deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        registryClient = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint);

        for(int i=1; i<=numberOfProjects; i++) {
            String projectName = String.format("project-%s", i);

            if (!kubernetes.namespaceExists(projectName)) {
                kubernetes.createNamespace(projectName);
            }
            IoTProject project = IoTUtils.getBasicIoTProjectObject(projectName, projectName, projectName);
            createIoTProject(project);
            IoTProjectTestContext ctx = new IoTProjectTestContext(projectName, project);

            configureDeviceSide(ctx);

            configureAmqpSide(ctx);

            projects.add(ctx);
        }
    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectHttpAdapterQdrProxyState();
        }

        SystemtestsKubernetesApps.deleteInfinispanServer(kubernetes.getInfraNamespace());

        for(IoTProjectTestContext ctx : projects) {
            cleanDeviceSide(ctx);
            cleanAmqpSide(ctx);
        }

    }

    @Test
    void testMultipleProjects() throws Exception {
        CompletableFuture.allOf(projects.stream()
                .map(ctx -> {
                    return CompletableFuture.allOf(
                            TestUtils.runAsync(() -> HttpAdapterTest.simpleHttpTelemetryTest(ctx.getAmqpClient(), tenantId(ctx.getProject()), ctx.getHttpAdapterClient())),
                            TestUtils.runAsync(() -> HttpAdapterTest.simpleHttpEventTest(ctx.getAmqpClient(), tenantId(ctx.getProject()), ctx.getHttpAdapterClient())),
                            TestUtils.runAsync(() -> MqttAdapterTest.simpleMqttTelemetryTest(ctx.getAmqpClient(), tenantId(ctx.getProject()), ctx.getMqttAdapterClient())));
                            //TODO add mqtt adapter tests when mqtt tests are enabled
                })
                .toArray(CompletableFuture[]::new)).get(5, TimeUnit.MINUTES);
    }

    private void configureAmqpSide(IoTProjectTestContext ctx) throws Exception {
        AddressSpace addressSpace = getAddressSpace(ctx.getNamespace(), ctx.getProject().getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());
        User amqpUser = configureAmqpUser(ctx.getProject(), addressSpace);
        ctx.setAmqpUser(amqpUser);
        AmqpClient amqpClient = configureAmqpClient(addressSpace, amqpUser);
        ctx.setAmqpClient(amqpClient);
    }

    private User configureAmqpUser(IoTProject project, AddressSpace addressSpace) {
        String tenant = tenantId(project);

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
        AmqpClient amqpClient = amqpClientFactory.createQueueClient(addressSpace);
        amqpClient.getConnectOptions()
            .setUsername(user.getSpec().getUsername())
            .setPassword(new String(Base64.getDecoder().decode(user.getSpec().getAuthentication().getPassword())));
        return amqpClient;
    }

    private void cleanAmqpSide(IoTProjectTestContext ctx) throws Exception {
        ctx.getAmqpClient().close();
        var userClient = kubernetes.getUserClient(ctx.getNamespace());
        userClient.delete(userClient.withName(ctx.getAmqpUser().getMetadata().getName()).get());
    }

    private void configureDeviceSide(IoTProjectTestContext ctx) throws Exception {
        String tenant = tenantId(ctx.getProject());
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
        String tenant = tenantId(ctx.getProject());
        String deviceId = ctx.getDeviceId();
        credentialsClient.deleteAllCredentials(tenant, deviceId);
        credentialsClient.getCredentials(tenant, deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        registryClient.deleteDeviceRegistration(tenant, deviceId);
        registryClient.getDeviceRegistration(tenant, deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        ctx.getHttpAdapterClient().close();
        if (ctx.getMqttAdapterClient().isConnected()) {
            ctx.getMqttAdapterClient().disconnect();
            ctx.getMqttAdapterClient().close();
        }
    }

}
