/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.PlansProvider;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.resources.AddressPlanDefinition;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlanDefinition;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PlansTest extends TestBaseWithShared implements ITestBaseStandard {

    private static Logger log = CustomLogger.getLogger();
    private static final PlansProvider plansProvider = new PlansProvider(kubernetes);

    @BeforeEach
    void setUp() {
        plansProvider.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        plansProvider.tearDown();
    }

    @Test
    @Disabled("test disabled because feature for appending address-plan is not implemented yet, issue: #904")
    void testAppendAddressPlan() throws Exception {
        List<AddressResource> addressResources = Collections.singletonList(new AddressResource("broker", 0.1));
        String weakQueuePlanName = "pooled-standard-queue-weak";
        AddressPlanDefinition weakQueuePlan = new AddressPlanDefinition(weakQueuePlanName, AddressType.QUEUE, addressResources);
        plansProvider.createAddressPlan(weakQueuePlan);

        AddressSpacePlanDefinition standardPlan = plansProvider.getAddressSpacePlan("standard");
        plansProvider.createAddressPlan(weakQueuePlan);
        standardPlan.getAddressPlans().add(weakQueuePlan);
        plansProvider.removeAddressSpacePlan(standardPlan);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 20;
        for (int i = 0; i < destCount; i++) {
            dest.add(AddressUtils.createQueueAddressObject("weak-queue-" + i, weakQueuePlan.getName()));
        }
        setAddresses(dest.toArray(new Address[0]));

        double requiredCredit = weakQueuePlan.getRequiredCreditFromResource("broker");
        int replicasCount = (int) (destCount * requiredCredit);
        waitForBrokerReplicas(sharedAddressSpace, dest.get(0), replicasCount);

        Future<List<Address>> standardAddresses = getAddressesObjects(Optional.empty()); //get all addresses
        for (int i = 0; i < destCount; i++) {
            assertThat("Queue plan wasn't set properly",
                    standardAddresses.get(20, TimeUnit.SECONDS).get(i).getSpec().getPlan(), is(weakQueuePlan.getName()));
        }
    }


}
