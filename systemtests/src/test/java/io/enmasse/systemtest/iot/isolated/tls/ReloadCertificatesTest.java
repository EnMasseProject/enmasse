/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.tls;

import static io.enmasse.systemtest.time.TimeoutBudget.ofDuration;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTShared;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class ReloadCertificatesTest extends TestBase implements ITestIoTShared {

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
    private KubernetesClient client;

    private static final String NAMESPACE = Kubernetes.getInstance().getInfraNamespace();

    @BeforeEach
    void initEnv() throws Exception {
        deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        registryClient = new DeviceRegistryClient(deviceRegistryEndpoint);
        credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint);
        registryClient.registerDevice(sharedIoTResourceManager.getTenantId(), deviceId);
        credentialsClient.addCredentials(sharedIoTResourceManager.getTenantId(), deviceId, deviceAuthId, devicePassword);

        User businessApplicationUser = UserUtils.createUserResource(new UserCredentials(businessApplicationUsername, businessApplicationPassword))
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withAddresses(
                                        IOT_ADDRESS_TELEMETRY + "/" + sharedIoTResourceManager.getTenantId(),
                                        IOT_ADDRESS_TELEMETRY + "/" + sharedIoTResourceManager.getTenantId() + "/*",
                                        IOT_ADDRESS_EVENT + "/" + sharedIoTResourceManager.getTenantId(),
                                        IOT_ADDRESS_EVENT + "/" + sharedIoTResourceManager.getTenantId() + "/*")
                                .withOperations(Operation.recv)
                                .build()))
                .endSpec()
                .done();

        AddressSpace addressSpace =
                resourcesManager.getAddressSpace(IOT_PROJECT_NAMESPACE, getSharedIoTProject().getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());

        resourcesManager.createOrUpdateUser(addressSpace, businessApplicationUser);

        businessApplicationClient = getAmqpClientFactory().createQueueClient(addressSpace);
        businessApplicationClient.getConnectOptions()
                .setUsername(businessApplicationUsername)
                .setPassword(businessApplicationPassword);

        Endpoint httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        adapterClient = new HttpAdapterClient(httpAdapterEndpoint, deviceAuthId, sharedIoTResourceManager.getTenantId(), devicePassword);

        client = kubernetes.getClient();
    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (credentialsClient != null) {
            credentialsClient.deleteAllCredentials(sharedIoTResourceManager.getTenantId(), deviceId);
        }
        if (registryClient != null) {
            registryClient.deleteDeviceRegistration(sharedIoTResourceManager.getTenantId(), deviceId);
            registryClient.getDeviceRegistration(sharedIoTResourceManager.getTenantId(), deviceId, HttpURLConnection.HTTP_NOT_FOUND);
        }
        if (adapterClient != null) {
            adapterClient.close();
        }
        var addressSpace = getSharedAddressSpace();
        if (addressSpace != null) {
            resourcesManager.removeUser(addressSpace, businessApplicationUsername);
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

    @Test
    @OpenShift(version = OpenShiftVersion.OCP4)
    public void testRecreateCertificate() throws Exception {

        // ensure everything works before starting

        assertTelemetryWorks();

        // get current pod

        var deploymentAccess = this.client
                .apps().deployments()
                .inNamespace(NAMESPACE)
                .withName("iot-http-adapter");

        var pod = this.client
                .pods()
                .inNamespace(NAMESPACE).withLabels(Map.of(
                        "app", "enmasse",
                        "name", "iot-http-adapter"))
                .list().getItems().stream()
                .map(p -> p.getMetadata().getName())
                .findFirst()
                .orElse(null);

        assertNotNull(pod);

        // then: reset http adapter key/cert

        var deleteResult = this.client.secrets()
                .inNamespace(NAMESPACE)
                .withName("iot-http-adapter-tls")
                .delete();

        assertEquals(Boolean.TRUE, deleteResult);

        final TimeoutBudget budget = ofDuration(ofMinutes(10));

        // wait until the deployment has changed

        var initialVersion = deploymentAccess.get().getMetadata().getResourceVersion();
        TestUtils.waitForChangedResourceVersion(budget, initialVersion, () -> {
            return Optional.ofNullable(deploymentAccess.get())
                    .map(d -> d.getMetadata().getResourceVersion())
                    .orElse(initialVersion);
        });

        // and wait until the IoTConfig is ready again

        IoTUtils.waitForIoTConfigReady(Kubernetes.getInstance(), getSharedIoTConfig());

        // now try to send messages again

        assertTelemetryWorks();
    }

    protected void assertTelemetryWorks() throws Exception {
        new MessageSendTester()
                .type(MessageSendTester.Type.TELEMETRY)
                .delay(Duration.ofMillis(500))
                .consumerFactory(ConsumerFactory.of(this.businessApplicationClient, sharedIoTResourceManager.getTenantId()))
                .sender(this.adapterClient::send)
                .amount(30)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }
}
