/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.project;

import static io.enmasse.systemtest.utils.AddressSpaceUtils.addressSpaceExists;
import static io.enmasse.systemtest.utils.TestUtils.waitUntilConditionOrFail;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.iot.model.v1.DoneableIoTProject;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.iot.model.v1.IoTProjectList;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTIsolated;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DefaultDeviceRegistry;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.Type;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class ManagedTest extends TestBase implements ITestIoTIsolated {

    private static final String MANAGED_TEST_ADDRESSSPACE = "managed-test-addrspace";

    private static final Logger log = CustomLogger.getLogger();

    private MixedOperation<IoTProject, IoTProjectList, DoneableIoTProject, Resource<IoTProject, DoneableIoTProject>> client;
    private MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> addressClient;
    private MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>> addressSpaceClient;
    private MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>> userClient;
    protected DeviceRegistryClient registryClient;
    protected CredentialsRegistryClient credentialsClient;

    private Endpoint httpAdapterEndpoint;
    private UserCredentials credentials;

    @BeforeEach
    public void initClients () throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();
        IoTConfig iotConfig = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewServices()
                .withDeviceRegistry(DefaultDeviceRegistry.newInfinispanBased())
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

        final Endpoint deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        this.registryClient = new DeviceRegistryClient(deviceRegistryEndpoint);
        this.credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint);
        this.client = kubernetes.getIoTProjectClient(IOT_PROJECT_NAMESPACE);
        this.addressClient = kubernetes.getAddressClient(IOT_PROJECT_NAMESPACE);
        this.addressSpaceClient = kubernetes.getAddressSpaceClient(IOT_PROJECT_NAMESPACE);
        this.userClient = kubernetes.getUserClient(IOT_PROJECT_NAMESPACE);
        this.httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");

        this.credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    @Test
    public void testChangeAddressSpace() throws Exception {

        var project = IoTUtils.getBasicIoTProjectObject("iot1", "as1",
                IOT_PROJECT_NAMESPACE, getDefaultAddressSpacePlan());
        isolatedIoTManager.createIoTProject(project);

        assertManagedResources(Assertions::assertNotNull, project, "as1");

        project = new IoTProjectBuilder(project)
                .editSpec()
                .editDownstreamStrategy()
                .editManagedStrategy()
                .editAddressSpace()

                .withName("as1a")

                .endAddressSpace()
                .endManagedStrategy()
                .endDownstreamStrategy()
                .endSpec()
                .build();

        // update the project

        log.info("Update project namespace");
        client.createOrReplace(project);

        // immediately after the change, the project is still ready but the new
        // address space is still missing, so we need to wait for it to be created
        // otherwise io.enmasse.systemtest.utils.IoTUtils.waitForIoTProjectReady(Kubernetes, IoTProject) will fail

        waitUntilConditionOrFail(
                addressSpaceExists(project.getMetadata().getNamespace(), project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName()),
                ofMinutes(5), ofSeconds(10),
                () -> String.format("Expected address space to be created"));

        // wait until the project and address space become ready

        log.info("For for project to become ready again");
        IoTUtils.waitForIoTProjectReady(kubernetes, project);

        // assert existence

        assertManagedResources(Assertions::assertNotNull, project, "as1a");
        assertManagedResources(Assertions::assertNull, project, "as1");
    }

    @Test
    public void testTwoManagedToTheSameAddressSpace() throws Exception {

        // first create two projects for a single address space

        var project1 = IoTUtils.getBasicIoTProjectObject("iot1", MANAGED_TEST_ADDRESSSPACE,
                IOT_PROJECT_NAMESPACE, getDefaultAddressSpacePlan());
        var project2 = IoTUtils.getBasicIoTProjectObject("iot2", MANAGED_TEST_ADDRESSSPACE,
                IOT_PROJECT_NAMESPACE, getDefaultAddressSpacePlan());

        var tenant1 = IoTUtils.getTenantId(project1);
        var tenant2 = IoTUtils.getTenantId(project2);

        // wait for the projects to be ready

        isolatedIoTManager.createIoTProject(project1);
        isolatedIoTManager.createIoTProject(project2);

        assertManagedResources(Assertions::assertNotNull, project1, MANAGED_TEST_ADDRESSSPACE);
        assertManagedResources(Assertions::assertNotNull, project2, MANAGED_TEST_ADDRESSSPACE);

        // register two devices with the same ids, but for different tenants

        this.registryClient.registerDevice(tenant1, "device1");
        this.registryClient.registerDevice(tenant2, "device1");
        this.credentialsClient.addPlainPasswordCredentials(tenant1, "device1", "auth1", "password1");
        this.credentialsClient.addPlainPasswordCredentials(tenant2, "device1", "auth1", "password1");

        // set up client

        isolatedIoTManager.createOrUpdateUser(isolatedIoTManager.getAddressSpace(IOT_PROJECT_NAMESPACE, MANAGED_TEST_ADDRESSSPACE), this.credentials);
        var iotAmqpClientFactory = new AmqpClientFactory(this.resourcesManager.getAddressSpace(IOT_PROJECT_NAMESPACE, MANAGED_TEST_ADDRESSSPACE), this.credentials);
        var amqpClient = iotAmqpClientFactory.createQueueClient();

        // now try to send some messages

        final List<Message> otherMessages = new LinkedList<>();
        try (
                var httpAdapterClient = new HttpAdapterClient(this.httpAdapterEndpoint, "auth1", tenant1, "password1");
                var otherReceiver = MessageSendTester.ConsumerFactory.of(amqpClient, tenant2).start(Type.TELEMETRY, msg -> otherMessages.add(msg)) ) {

            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .amount(1)
                    .consumerFactory(MessageSendTester.ConsumerFactory.of(amqpClient, tenant1))
                    .sender(httpAdapterClient::send)
                    .execute();

        }

        assertThat(otherMessages, emptyIterable());
    }

    private void assertManagedResources(final BiConsumer<Object,String> assertor, final IoTProject project, final String addressSpaceName) {
        assertObject(assertor, "Address space", this.addressSpaceClient, addressSpaceName);
        assertObject(assertor, "Adapter user", this.userClient, addressSpaceName + ".adapter-" + project.getMetadata().getUid());
        for ( final String address : IOT_ADDRESSES) {
            var addressName = address + "/" + IOT_PROJECT_NAMESPACE + "." + project.getMetadata().getName();
            var metaName = KubeUtil.sanitizeForGo(addressSpaceName, addressName);
            assertObject(assertor, "Address: " + addressName + " / " + metaName, this.addressClient, metaName);
        }
    }

    private static void assertObject(final BiConsumer<Object,String> assertor, final String message, final MixedOperation<? extends Object, ?, ?, ?> client, final String name) {
        var object = client.withName(name).get();
        assertor.accept(object, message);
    }

}
