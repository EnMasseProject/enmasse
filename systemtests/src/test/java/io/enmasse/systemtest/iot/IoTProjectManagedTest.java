/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.IoTTestBase;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorization;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

@Tag(TestTag.sharedIot)
@Tag(TestTag.smoke)
class IoTProjectManagedTest extends IoTTestBase implements ITestBaseStandard {

    @Test
    void testCreate() throws Exception {
        createIoTConfig(new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withEnableDefaultRoutes(false)
                .withNewServices()
                .withNewDeviceRegistry()
                .withNewFile()
                .endFile()
                .endDeviceRegistry()
                .endServices()
                .endSpec()
                .build());

        String addressSpaceName = "managed-address-space";

        IoTProject project = IoTUtils.getBasicIoTProjectObject("iot-project-managed", addressSpaceName, this.iotProjectNamespace);

        createIoTProject(project);// waiting until ready

        var iotProjectApiClient = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace());
        IoTProject created = iotProjectApiClient.withName(project.getMetadata().getName()).get();

        assertNotNull(created);
        assertEquals(iotProjectNamespace, created.getMetadata().getNamespace());
        assertEquals(project.getMetadata().getName(), created.getMetadata().getName());
        assertEquals(
                project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName(),
                created.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());

        assertManaged(created);

    }

    private void assertManaged(IoTProject project) throws Exception {
        //address space s
        AddressSpace addressSpace = getAddressSpace(iotProjectNamespace, project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());
        assertEquals(project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName(), addressSpace.getMetadata().getName());
        assertEquals("standard", addressSpace.getSpec().getType());
        assertEquals("standard-unlimited", addressSpace.getSpec().getPlan());

        //addresses
        //{event/control/telemetry}/"project-namespace"."project-name"
        String addressSuffix = "/" + project.getMetadata().getNamespace() + "." + project.getMetadata().getName();
        List<Address> addresses = AddressUtils.getAddresses(addressSpace);
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
        User user = getUser(addressSpace, "adapter");
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

}
