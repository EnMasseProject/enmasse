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
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.IoTTestBase;
import io.enmasse.systemtest.iot.http.HttpAdapterTest;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
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
    private List<MultipleIoTProjectsTestContext> projects = new ArrayList<>();

    @BeforeEach
    void initEnv() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();
        IoTConfig iotConfig = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
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
            IoTProject project = createProject(projectName);

            HttpAdapterClient httpAdapterClient = configureDeviceSide(project);

            AddressSpace addressSpace = getAddressSpace(project.getMetadata().getNamespace(), project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());
            User amqpUser = configureAmqpUser(project, addressSpace);
            AmqpClient amqpClient = configureAmqpClient(addressSpace, amqpUser);

            projects.add(new MultipleIoTProjectsTestContext(project, httpAdapterClient, amqpClient, amqpUser));
        }
    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectHttpAdapterQdrProxyState();
        }

        for(MultipleIoTProjectsTestContext ctx : projects) {
            cleanDeviceSide(ctx);
            cleanAmqpSide(ctx);
        }

    }

    @Test
    void testMultipleProjectsSequentially() throws Exception {
        for(MultipleIoTProjectsTestContext ctx : projects) {
            HttpAdapterTest.simpleHttpTelemetryTest(ctx.getAmqpClient(), tenantId(ctx.getProject()), ctx.getHttpAdapterClient());
        }
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
        AmqpClient project1AmqpClient = amqpClientFactory.createQueueClient(addressSpace);
        project1AmqpClient.getConnectOptions()
            .setUsername(user.getSpec().getUsername())
            .setPassword(new String(Base64.getDecoder().decode(user.getSpec().getAuthentication().getPassword())));
        return project1AmqpClient;
    }

    private void cleanAmqpSide(MultipleIoTProjectsTestContext ctx) throws Exception {
        ctx.getAmqpClient().close();
        var userClient = kubernetes.getUserClient(ctx.getProject().getMetadata().getNamespace());
        userClient.delete(userClient.withName(ctx.getAmqpUser().getMetadata().getName()).get());
    }

    private HttpAdapterClient configureDeviceSide(IoTProject project) throws Exception {
        String tenant = tenantId(project);
        String deviceId = UUID.randomUUID().toString();
        String deviceAuthId = UUID.randomUUID().toString();
        String devicePassword = UUID.randomUUID().toString();
        registryClient.registerDevice(tenant, deviceId);
        credentialsClient.addCredentials(tenant, deviceId, deviceAuthId, devicePassword);
        Endpoint httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        return new HttpAdapterClient(kubernetes, httpAdapterEndpoint, deviceAuthId, tenant, devicePassword);
    }

    private void cleanDeviceSide(MultipleIoTProjectsTestContext ctx) throws Exception {
        String tenant = tenantId(ctx.getProject());
        String deviceId = ctx.getProject().getMetadata().getName();
        credentialsClient.deleteAllCredentials(tenant, deviceId);
        credentialsClient.getCredentials(tenant, deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        registryClient.deleteDeviceRegistration(tenant, deviceId);
        registryClient.getDeviceRegistration(tenant, deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        ctx.getHttpAdapterClient().close();
    }

    private IoTProject createProject(String projectName) throws Exception {
        if (!kubernetes.namespaceExists(projectName)) {
            kubernetes.createNamespace(projectName);
        }
        IoTProject project = IoTUtils.getBasicIoTProjectObject(projectName, projectName, projectName);
        createIoTProject(project);
        return project;
    }

}
