/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.DoneableAddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.AdminResourcesManager;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PlansTest extends TestBaseWithShared implements ITestBaseStandard {

    private static final AdminResourcesManager adminManager = new AdminResourcesManager();

    @BeforeEach
    void setUp() {
        adminManager.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        adminManager.tearDown();
    }

    @Test
    @Disabled("test disabled because feature for appending address-plan is not implemented yet, issue: #904")
    void testAppendAddressPlan() throws Exception {
        List<ResourceRequest> addressResources = Collections.singletonList(new ResourceRequest("broker", 0.1));
        String weakQueuePlanName = "pooled-standard-queue-weak";
        AddressPlan weakQueuePlan = PlanUtils.createAddressPlanObject(weakQueuePlanName, AddressType.QUEUE, addressResources);
        adminManager.createAddressPlan(weakQueuePlan);

        AddressSpacePlan standardPlan = adminManager.getAddressSpacePlan("standard");
        adminManager.createAddressPlan(weakQueuePlan);
        standardPlan = new DoneableAddressSpacePlan(standardPlan).addNewAddressPlan(weakQueuePlanName).done();
        adminManager.removeAddressSpacePlan(standardPlan);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 20;
        for (int i = 0; i < destCount; i++) {
            dest.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "weak-queue-" + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("weak-queue-" + i)
                    .withPlan(weakQueuePlan.getMetadata().getName())
                    .endSpec()
                    .build());
        }
        setAddresses(dest.toArray(new Address[0]));

        double requiredCredit = PlanUtils.getRequiredCreditFromAddressResource("broker", weakQueuePlan);
        int replicasCount = (int) (destCount * requiredCredit);
        waitForBrokerReplicas(sharedAddressSpace, dest.get(0), replicasCount);

        List<Address> standardAddresses = kubernetes.getAddressClient().inAnyNamespace().list().getItems(); //get all addresses
        for (int i = 0; i < destCount; i++) {
            assertThat("Queue plan wasn't set properly",
                    standardAddresses.get(i).getSpec().getPlan(), is(weakQueuePlan.getMetadata().getName()));
        }
    }


}
