/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.CommonAdapterContainersBuilder;
import io.enmasse.iot.model.v1.ContainerConfigBuilder;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.IoTTestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorization;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Quantity;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TestTag.smoke;
import static io.enmasse.systemtest.TimeoutBudget.ofDuration;
import static java.time.Duration.ofMinutes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

@Tag(sharedIot)
@Tag(smoke)
class IoTProjectManagedTest extends IoTTestBase implements ITestBaseStandard {

    @Test
    void testCreate() throws Exception {

        var config = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withEnableDefaultRoutes(false)
                .endSpec();

        if (Environment.getInstance().useMinikube()) {
            config = overrideCertificates(config);
            config = overrideResourceLimits(config);
        }

        createIoTConfig(config.build());

        String addressSpaceName = "managed-address-space";

        IoTProject project = IoTUtils.getBasicIoTProjectObject("iot-project-managed", addressSpaceName);

        try {
            createIoTProject(project);// waiting until ready
        } catch (Exception e) {
            TestUtils.streamNonReadyPods(Kubernetes.getInstance(), config.buildMetadata().getName()).forEach(KubeCMDClient::dumpPodLogs);
            throw e;
        }

        // the adapter user may take a bit to appear

        TestUtils.waitUntilCondition("Adapter user created", () -> {
            try {
                getAdapterUser(addressSpaceName);
                return true;
            } catch (Exception e) {
                return false;
            }
        }, ofDuration(ofMinutes(5)));

        // assert

        IoTProject created = iotProjectApiClient.getIoTProject(project.getMetadata().getName());

        assertNotNull(created);
        assertEquals(iotProjectNamespace, created.getMetadata().getNamespace());
        assertEquals(project.getMetadata().getName(), created.getMetadata().getName());
        assertEquals(
                project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName(),
                created.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());

        assertManaged(created);

    }

    /**
     * Set systemtest TLS material
     */
    private IoTConfigBuilder overrideCertificates(IoTConfigBuilder config) {

        final Map<String, String> secrets = new HashMap<>();
        secrets.put("iot-auth-service", "systemtests-iot-auth-service-tls");
        secrets.put("iot-tenant-service", "systemtests-iot-tenant-service-tls");
        secrets.put("iot-device-registry", "systemtests-iot-device-registry-tls");

        return config.editOrNewSpec()

                .editOrNewInterServiceCertificates()
                .editOrNewSecretCertificatesStrategy()

                .withCaSecretName("systemtests-iot-service-ca")
                .withServiceSecretNames(secrets)

                .endSecretCertificatesStrategy()
                .endInterServiceCertificates()

                .editOrNewAdapters()

                .editOrNewHttp()
                .editOrNewEndpoint().withNewSecretNameStrategy("systemtests-iot-http-adapter-tls").endEndpoint()
                .endHttp()

                .editOrNewMqtt()
                .withNewEndpoint().withNewSecretNameStrategy("systemtests-iot-mqtt-adapter-tls").endEndpoint()
                .endMqtt()

                .endAdapters()

                .endSpec();

    }

    private IoTConfigBuilder overrideResourceLimits(IoTConfigBuilder config) {

        var r1 = new ContainerConfigBuilder()
                .withNewResources().addToLimits("memory", new Quantity("64Mi")).endResources()
                .build();
        var r2 = new ContainerConfigBuilder()
                .withNewResources().addToLimits("memory", new Quantity("256Mi")).endResources()
                .build();

        var commonContainers = new CommonAdapterContainersBuilder()
                .withNewAdapterLike(r2).endAdapter()
                .withNewProxyLike(r1).endProxy()
                .withNewProxyConfiguratorLike(r1).endProxyConfigurator()
                .build();

        return config
                .editOrNewSpec()

                .editOrNewAdapters()

                .editOrNewHttp()
                .withNewContainersLike(commonContainers).endContainers()
                .endHttp()

                .editOrNewMqtt()
                .withNewContainersLike(commonContainers).endContainers()
                .endMqtt()

                .endAdapters()

                .editOrNewServices()

                .editOrNewAuthentication()
                .editOrNewContainerLike(r2).endContainer()
                .endAuthentication()

                .editOrNewTenant()
                .editOrNewContainerLike(r2).endContainer()
                .endTenant()

                .editOrNewCollector()
                .editOrNewContainerLike(r1).endContainer()
                .endCollector()

                .editOrNewDeviceRegistry()
                .editOrNewFile()
                .editOrNewContainerLike(r2).endContainer()
                .endFile()
                .endDeviceRegistry()

                .endServices()
                .endSpec();
    }

    private void assertManaged(IoTProject project) throws Exception {
        //address space s
        AddressSpace addressSpace = getAddressSpace(project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());
        assertEquals(project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName(), addressSpace.getMetadata().getName());
        assertEquals("standard", addressSpace.getSpec().getType());
        assertEquals("standard-unlimited", addressSpace.getSpec().getPlan());

        //addresses
        //{event/control/telemetry}/"project-namespace"."project-name"
        String addressSuffix = "/" + project.getMetadata().getNamespace() + "." + project.getMetadata().getName();
        List<Address> addresses = getAddressesObjects(addressSpace, Optional.empty()).get(30, TimeUnit.SECONDS);
        assertEquals(3, addresses.size());
        assertEquals(3, addresses.stream()
                .map(Address::getMetadata)
                .map(ObjectMeta::getOwnerReferences)
                .flatMap(List::stream)
                .filter(reference -> isOwner(project, reference))
                .count());
        int correctAddressesCounter = 0;
        for (Address address : addresses) {
            if (address.getSpec().getAddress().equals(IOT_ADDRESS_EVENT + addressSuffix)) {
                assertEquals("queue", address.getSpec().getType());
                assertEquals("standard-small-queue", address.getSpec().getPlan());
                correctAddressesCounter++;
            } else if (address.getSpec().getAddress().equals(IOT_ADDRESS_CONTROL + addressSuffix)
                    || address.getSpec().getAddress().equals(IOT_ADDRESS_TELEMETRY + addressSuffix)) {
                assertEquals("anycast", address.getSpec().getType());
                assertEquals("standard-small-anycast", address.getSpec().getPlan());
                correctAddressesCounter++;
            }
        }
        assertEquals(3, correctAddressesCounter, "There are incorrect IoT addresses " + addresses);

        //username "adapter"
        //name "project-address-space"+".adapter"
        User user = getAdapterUser(addressSpace.getMetadata().getName());

        assertNotNull(user);
        assertEquals(1, user.getMetadata().getOwnerReferences().size());
        assertTrue(isOwner(project, user.getMetadata().getOwnerReferences().get(0)));

        UserAuthorization actualAuthorization = user.getSpec().getAuthorization().stream().findFirst().get();

        assertThat(actualAuthorization.getOperations(), containsInAnyOrder(Operation.recv, Operation.send));

        assertThat(actualAuthorization.getAddresses(), containsInAnyOrder(IOT_ADDRESS_EVENT + addressSuffix,
                IOT_ADDRESS_CONTROL + addressSuffix,
                IOT_ADDRESS_TELEMETRY + addressSuffix,
                IOT_ADDRESS_EVENT + addressSuffix + "/*",
                IOT_ADDRESS_CONTROL + addressSuffix + "/*",
                IOT_ADDRESS_TELEMETRY + addressSuffix + "/*"));
    }

    private boolean isOwner(IoTProject project, OwnerReference ownerReference) {
        return ownerReference.getKind().equals(IoTProject.KIND) && project.getMetadata().getName().equals(ownerReference.getName());
    }

    private User getAdapterUser (String addressSpaceName) throws Exception {
        return UserUtils.getUserObject(getUserApiClient(), addressSpaceName, "adapter");
    }

}
