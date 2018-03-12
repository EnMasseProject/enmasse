/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.IsolatedAddressSpace;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.resources.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

@Category(IsolatedAddressSpace.class)
public class AddressControllerApiTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();

    @Override
    protected String getDefaultPlan(AddressType addressType) {
        return null;
    }

    @Before
    public void setUp() {
        plansProvider.setUp();
    }

    @After
    public void tearDown() {
        plansProvider.tearDown();
    }

    @Test
    public void testRestApiGetSchema() throws Exception {
        AddressPlan queuePlan = new AddressPlan("test-schema-rest-api-addr-plan", AddressType.QUEUE,
                Collections.singletonList(new AddressResource("broker", 0.6)));
        plansProvider.createAddressPlanConfig(queuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 0.0, 2.0),
                new AddressSpaceResource("router", 1.0, 1.0),
                new AddressSpaceResource("aggregate", 0.0, 2.0));
        List<AddressPlan> addressPlans = Collections.singletonList(queuePlan);
        AddressSpacePlan addressSpacePlan = new AddressSpacePlan("schema-rest-api-plan", "schema-rest-api-plan",
                "standard-space", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlanConfig(addressSpacePlan);

        Future<SchemaData> data = getSchema();
        SchemaData schemaData = data.get(20, TimeUnit.SECONDS);
        log.info("Check if schema object is not null");
        assertThat(schemaData.getAddressSpaceTypes().size(), not(0));

        log.info("Check if schema object contains new address space plan");
        assertTrue(schemaData.getAddressSpaceType("standard").getPlans()
                .stream()
                .map(PlanData::getName)
                .collect(Collectors.toList()).contains("schema-rest-api-plan"));

        log.info("Check if schema contains new address plans");
        assertTrue(schemaData.getAddressSpaceType("standard").getAddressType("queue").getPlans().stream()
                .filter(s -> s.getName().equals("test-schema-rest-api-addr-plan"))
                .map(PlanData::getName)
                .collect(Collectors.toList())
                .contains("test-schema-rest-api-addr-plan"));
    }
}
