/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.project;

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
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTIsolated;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.iot.DefaultDeviceRegistry;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import static io.enmasse.systemtest.utils.AddressSpaceUtils.addressSpaceExists;
import static io.enmasse.systemtest.utils.TestUtils.waitUntilConditionOrFail;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

class ManagedTest extends TestBase implements ITestIoTIsolated {

    private MixedOperation<IoTProject, IoTProjectList, DoneableIoTProject, Resource<IoTProject, DoneableIoTProject>> client;
    private MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> addressClient;
    private MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>> addressSpaceClient;
    private MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>> userClient;
    private static final Logger LOGGER = CustomLogger.getLogger();

    @BeforeEach
    void initClients() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();
        IoTConfig iotConfig = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .withNamespace(KUBERNETES.getInfraNamespace())
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

        ISOLATED_IOT_MANAGER.createIoTConfig(iotConfig);

        this.client = KUBERNETES.getIoTProjectClient(IOT_PROJECT_NAMESPACE);
        this.addressClient = KUBERNETES.getAddressClient(IOT_PROJECT_NAMESPACE);
        this.addressSpaceClient = KUBERNETES.getAddressSpaceClient(IOT_PROJECT_NAMESPACE);
        this.userClient = KUBERNETES.getUserClient(IOT_PROJECT_NAMESPACE);
    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectHttpAdapterQdrProxyState();
        }
        SystemtestsKubernetesApps.deleteInfinispanServer(KUBERNETES.getInfraNamespace());
    }

    @Test
    void testChangeAddressSpace() throws Exception {

        var project = IoTUtils.getBasicIoTProjectObject("iot1", "as1",
                IOT_PROJECT_NAMESPACE, getDefaultAddressSpacePlan());
        ISOLATED_IOT_MANAGER.createIoTProject(project);

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

        LOGGER.info("Update project namespace");
        client.createOrReplace(project);

        // immediately after the change, the project is still ready but the new
        // address space is still missing, so we need to wait for it to be created
        // otherwise io.enmasse.systemtest.utils.IoTUtils.waitForIoTProjectReady(Kubernetes, IoTProject) will fail

        waitUntilConditionOrFail(
                addressSpaceExists(project.getMetadata().getNamespace(), project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName()),
                ofMinutes(5), ofSeconds(10),
                () -> "Expected address space to be created");

        // wait until the project and address space become ready

        LOGGER.info("For for project to become ready again");
        IoTUtils.waitForIoTProjectReady(KUBERNETES, project);

        // assert existence

        assertManagedResources(Assertions::assertNotNull, project, "as1a");
        assertManagedResources(Assertions::assertNull, project, "as1");

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

    private static void assertObject(final BiConsumer<Object,String> assertor, final String message, final MixedOperation<?, ?, ?, ?> client, final String name) {
        var object = client.withName(name).get();
        assertor.accept(object, message);
    }

}
