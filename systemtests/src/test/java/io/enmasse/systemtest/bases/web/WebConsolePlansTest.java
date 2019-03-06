/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.Address;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.resources.AddressPlanDefinition;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlanDefinition;
import io.enmasse.systemtest.resources.AddressSpaceResource;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.standard.TopicTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(isolated)
public abstract class WebConsolePlansTest extends TestBase implements ISeleniumProvider {

    private static Logger log = CustomLogger.getLogger();
    private static final PlansProvider plansProvider = new PlansProvider(kubernetes);

    private ConsoleWebPage consoleWebPage;

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        selenium.setupDriver(environment, kubernetes, buildDriver());
        plansProvider.setUp();
    }

    @AfterEach
    public void tearDownDrivers() throws Exception {
        selenium.tearDownDrivers();
        plansProvider.tearDown();
    }

    //============================================================================================
    //============================ do test methods ===============================================
    //============================================================================================

    /**
     * related github issue: #921
     */
    protected void doTestCreateAddressPlan() throws Exception {
        //define and create address plans
        List<AddressResource> addressResourcesQueue1 = Arrays.asList(new AddressResource("broker", 0.15), new AddressResource("router", 0.0));
        List<AddressResource> addressResourcesTopic2 = Arrays.asList(
                new AddressResource("broker", 0.3),
                new AddressResource("router", 0.2));
        List<AddressResource> addressResourcesQueue3 = Arrays.asList(new AddressResource("broker", 0.25), new AddressResource("router", 0.0));
        AddressPlanDefinition consoleQueuePlan1 = new AddressPlanDefinition("console-queue-1", AddressType.QUEUE, addressResourcesQueue1);
        AddressPlanDefinition consoleTopicPlan2 = new AddressPlanDefinition("console-topic-2", AddressType.TOPIC, addressResourcesTopic2);
        AddressPlanDefinition consoleQueuePlan3 = new AddressPlanDefinition("console-queue-3", AddressType.QUEUE, addressResourcesQueue3);

        plansProvider.createAddressPlan(consoleQueuePlan1);
        plansProvider.createAddressPlan(consoleTopicPlan2);
        plansProvider.createAddressPlan(consoleQueuePlan3);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 3.0),
                new AddressSpaceResource("router", 5.0),
                new AddressSpaceResource("aggregate", 8.0));
        List<AddressPlanDefinition> addressPlans = Arrays.asList(consoleQueuePlan1, consoleTopicPlan2, consoleQueuePlan3);
        AddressSpacePlanDefinition consolePlan = new AddressSpacePlanDefinition("console-plan",
                "default-with-mqtt", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(consolePlan);

        //create address space plan with new plan
        AddressSpace consoleAddrSpace = new AddressSpace("console-plan-instance", AddressSpaceType.STANDARD,
                consolePlan.getName(), AuthService.STANDARD);
        createAddressSpace(consoleAddrSpace);

        //create new user
        UserCredentials user = new UserCredentials("test-newplan-name", "test_newplan_password");
        createUser(consoleAddrSpace, user);

        //create addresses
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(consoleAddrSpace), addressApiClient, consoleAddrSpace, user);
        consoleWebPage.openWebConsolePage();
        Address q1 = AddressUtils.createQueue("new-queue-instance-1", consoleQueuePlan1.getName());
        Address t2 = AddressUtils.createTopic("new-topic-instance-2", consoleTopicPlan2.getName());
        Address q3 = AddressUtils.createQueue("new-queue-instance-3", consoleQueuePlan3.getName());
        consoleWebPage.createAddressesWebConsole(q1, t2, q3);

        String assertMessage = "Address plan wasn't set properly";
        assertEquals(q1.getSpec().getPlan(), consoleWebPage.getAddressItem(q1).getPlan(), assertMessage);
        assertEquals(t2.getSpec().getPlan(), consoleWebPage.getAddressItem(t2).getPlan(), assertMessage);
        assertEquals(q3.getSpec().getPlan(), consoleWebPage.getAddressItem(q3).getPlan(), assertMessage);

        //simple send/receive
        amqpClientFactory = new AmqpClientFactory(kubernetes, environment, consoleAddrSpace, user);
        AmqpClient queueClient = amqpClientFactory.createQueueClient(consoleAddrSpace);
        queueClient.getConnectOptions().setCredentials(user);

        AmqpClient topicClient = amqpClientFactory.createTopicClient(consoleAddrSpace);
        topicClient.getConnectOptions().setCredentials(user);

        QueueTest.runQueueTest(queueClient, q1, 333);
        TopicTest.runTopicTest(topicClient, t2, 333);
        QueueTest.runQueueTest(queueClient, q3, 333);

        consoleWebPage.deleteAddressWebConsole(t2); //remaining addresses will be removed automatically via tearDown
    }
}
