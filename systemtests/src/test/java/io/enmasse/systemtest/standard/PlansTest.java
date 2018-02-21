/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.StandardTestBase;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.AddressSpaceResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PlansTest extends StandardTestBase {

    private static Logger log = CustomLogger.getLogger();

    @Before
    public void setUp() {
        plansProvider.setUp();
    }

    @After
    public void tearDown() {
        plansProvider.tearDown();
    }

    //@Test //test disabled because feature for appending address-plan is not implemented yet, issue: #904
    public void testAppendAddressPlan() throws Exception {
        List<AddressResource> addressResources = Arrays.asList(new AddressResource("broker", 0.1));
        String weakQueuePlanName = "pooled-standard-queue-weak";
        AddressPlan weakQueuePlan = new AddressPlan(weakQueuePlanName, AddressType.QUEUE, addressResources);
        plansProvider.createAddressPlanConfig(weakQueuePlan);

        AddressSpacePlan standardPlan = getAddressSpacePlanConfig("standard");
        plansProvider.appendAddressPlan(weakQueuePlan, standardPlan);

        ArrayList<Destination> dest = new ArrayList<>();
        int destCount = 20;
        for (int i = 0; i < destCount; i++) {
            dest.add(Destination.queue("weak-queue-" + i, weakQueuePlan.getName()));
        }
        setAddresses(dest.toArray(new Destination[0]));

        double requiredCredit = weakQueuePlan.getRequiredCreditFromResource("broker");
        int replicasCount = (int) (destCount * requiredCredit);
        waitForBrokerReplicas(sharedAddressSpace, dest.get(0), replicasCount);

        Future<List<Address>> standardAddresses = getAddressesObjects(Optional.empty()); //get all addresses
        for (int i = 0; i < destCount; i++) {
            assertThat("Queue plan wasn't set properly",
                    standardAddresses.get(20, TimeUnit.SECONDS).get(i).getPlan(), is(weakQueuePlan.getName()));
        }
    }


}
