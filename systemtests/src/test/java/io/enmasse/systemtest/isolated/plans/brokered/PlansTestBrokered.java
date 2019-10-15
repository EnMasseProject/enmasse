/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.plans.brokered;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedBrokered;
import io.enmasse.systemtest.utils.MessagingUtils;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

public class PlansTestBrokered extends TestBase implements ITestIsolatedBrokered {
    private MessagingUtils clientUtils = getClientUtils();

    @Test
    void testReplaceAddressSpacePlanBrokered() throws Exception {
        //define and create address plans
        AddressPlan beforeQueuePlan = PlanUtils.createAddressPlanObject("small-queue", AddressType.QUEUE,
                Collections.singletonList(new ResourceRequest("broker", 0.4)));

        AddressPlan afterQueuePlan = PlanUtils.createAddressPlanObject("bigger-queue", AddressType.QUEUE,
                Collections.singletonList(new ResourceRequest("broker", 0.7)));

        isolatedResourcesManager.createAddressPlan(beforeQueuePlan);
        isolatedResourcesManager.createAddressPlan(afterQueuePlan);

        //define and create address space plans

        AddressSpacePlan beforeAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("before-update-brokered-plan",
                "default", AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 5.0)),
                Collections.singletonList(beforeQueuePlan));

        AddressSpacePlan afterAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("after-update-brokered-plan",
                "default", AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 5.0)),
                Collections.singletonList(afterQueuePlan));

        isolatedResourcesManager.createAddressSpacePlan(beforeAddressSpacePlan);
        isolatedResourcesManager.createAddressSpacePlan(afterAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-brokered-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(beforeAddressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-1"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-1")
                .withPlan(beforeQueuePlan.getMetadata().getName())
                .endSpec()
                .build();
        resourcesManager.setAddresses(queue);

        clientUtils.sendDurableMessages(resourcesManager, addressSpace, queue, user, 16);

        addressSpace = new DoneableAddressSpace(addressSpace).editSpec().withPlan(afterAddressSpacePlan.getMetadata().getName()).endSpec().done();
        isolatedResourcesManager.replaceAddressSpace(addressSpace);

        clientUtils.receiveDurableMessages(resourcesManager, addressSpace, queue, user, 16);

        Address afterQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-2")
                .withPlan(afterQueuePlan.getMetadata().getName())
                .endSpec()
                .build();
        resourcesManager.appendAddresses(afterQueue);

        getClientUtils().assertCanConnect(addressSpace, user, Arrays.asList(afterQueue, queue), resourcesManager);
    }
}
