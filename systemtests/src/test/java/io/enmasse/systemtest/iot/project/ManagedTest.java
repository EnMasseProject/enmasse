/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.project;

import java.util.function.BiConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.iot.model.v1.DoneableIoTProject;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.iot.model.v1.IoTProjectList;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

@Tag(TestTag.SHARED_IOT)
public class ManagedTest extends AbstractIoTProjectTestBase  {

    private MixedOperation<IoTProject, IoTProjectList, DoneableIoTProject, Resource<IoTProject, DoneableIoTProject>> client;
    private MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> addressClient;
    private MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>> addressSpaceClient;
    private MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>> userClient;

    @BeforeEach
    public void initClients () {
        this.client = kubernetes.getIoTProjectClient(this.iotProjectNamespace);
        this.addressClient = kubernetes.getAddressClient(this.iotProjectNamespace);
        this.addressSpaceClient = kubernetes.getAddressSpaceClient(this.iotProjectNamespace);
        this.userClient = kubernetes.getUserClient(this.iotProjectNamespace);
    }

    @Test
    public void testChangeAddressSpace() throws Exception {

        var project = IoTUtils.getBasicIoTProjectObject("iot1", "as1", this.iotProjectNamespace);
        createIoTProject(project);

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

        client.createOrReplace(project);

        IoTUtils.waitForIoTProjectReady(kubernetes, project);
        IoTUtils.syncIoTProject(kubernetes, project);

        assertManagedResources(Assertions::assertNotNull, project, "as1a");
        assertManagedResources(Assertions::assertNull, project, "as1");

    }

    private void assertManagedResources(final BiConsumer<Object,String> assertor, final IoTProject project, final String addressSpaceName) {
        assertObject(assertor, "Address space", this.addressSpaceClient, addressSpaceName);
        assertObject(assertor, "Adapter user", this.userClient, addressSpaceName + ".adapter-" + project.getMetadata().getUid());
        for ( final String address : IOT_ADDRESSES) {
            var name = address + "/" + this.iotProjectNamespace + "." + project.getMetadata().getName();
            assertObject(assertor, "Address: " + name, this.addressClient, generateNameForGo(addressSpaceName, name));
        }
    }

    private static void assertObject(final BiConsumer<Object,String> assertor, final String message, final MixedOperation<? extends Object, ?, ?, ?> client, final String name) {
        var object = client.withName(name).get();
        assertor.accept(object, message);
    }

}
