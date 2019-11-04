/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.DoneableAddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedStandard;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PlansTest extends TestBase implements ITestSharedStandard {

    @Test
    @Disabled("test disabled because feature for appending address-plan is not implemented yet, issue: #904")
    void testAppendAddressPlan() throws Exception {
        List<ResourceRequest> addressResources = Collections.singletonList(new ResourceRequest("broker", 0.1));
        String weakQueuePlanName = "pooled-standard-queue-weak";
        AddressPlan weakQueuePlan = PlanUtils.createAddressPlanObject(weakQueuePlanName, AddressType.QUEUE, addressResources);
        resourcesManager.createAddressPlan(weakQueuePlan);

        AddressSpacePlan standardPlan = resourcesManager.getAddressSpacePlan("standard");
        resourcesManager.createAddressPlan(weakQueuePlan);
        standardPlan = new DoneableAddressSpacePlan(standardPlan).addNewAddressPlan(weakQueuePlanName).done();
        resourcesManager.removeAddressSpacePlan(standardPlan);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 20;
        for (int i = 0; i < destCount; i++) {
            dest.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "weak-queue-" + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("weak-queue-" + i)
                    .withPlan(weakQueuePlan.getMetadata().getName())
                    .endSpec()
                    .build());
        }
        resourcesManager.setAddresses(dest.toArray(new Address[0]));

        double requiredCredit = PlanUtils.getRequiredCreditFromAddressResource("broker", weakQueuePlan);
        int replicasCount = (int) (destCount * requiredCredit);
        TestUtils.waitForBrokerReplicas(getSharedAddressSpace(), dest.get(0), replicasCount);

        List<Address> standardAddresses = KUBERNETES.getAddressClient().inAnyNamespace().list().getItems(); //get all addresses
        for (int i = 0; i < destCount; i++) {
            assertThat("Queue plan wasn't set properly",
                    standardAddresses.get(i).getSpec().getPlan(), is(weakQueuePlan.getMetadata().getName()));
        }
    }


}
