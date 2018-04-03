/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.resolvers.ExtensionContextParameterResolver;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.AddressSpaceResource;
import io.enmasse.systemtest.selenium.ConsoleWebPage;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.standard.TopicTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("isolated")
@ExtendWith(ExtensionContextParameterResolver.class)
public abstract class WebConsolePlansTest extends TestBase implements ISeleniumProvider {

    private static Logger log = CustomLogger.getLogger();
    private SeleniumProvider selenium = new SeleniumProvider();

    private ConsoleWebPage consoleWebPage;

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        Thread.sleep(30000); //sleep before run test (until geckodriver will be fixed)
        selenium.setupDriver(environment, kubernetes, buildDriver());
        plansProvider.setUp();
    }

    @AfterEach
    public void tearDownDrivers(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            selenium.onFailed(context);
        } else {
            selenium.tearDownDrivers();
        }
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
        List<AddressResource> addressResourcesQueue1 = Arrays.asList(new AddressResource("broker", 0.15));
        List<AddressResource> addressResourcesTopic2 = Arrays.asList(
                new AddressResource("broker", 0.3),
                new AddressResource("router", 0.2));
        List<AddressResource> addressResourcesQueue3 = Arrays.asList(new AddressResource("broker", 0.25));
        AddressPlan consoleQueuePlan1 = new AddressPlan("console-queue-1", AddressType.QUEUE, addressResourcesQueue1);
        AddressPlan consoleTopicPlan2 = new AddressPlan("console-topic-2", AddressType.TOPIC, addressResourcesTopic2);
        AddressPlan consoleQueuePlan3 = new AddressPlan("console-queue-3", AddressType.QUEUE, addressResourcesQueue3);

        plansProvider.createAddressPlanConfig(consoleQueuePlan1);
        plansProvider.createAddressPlanConfig(consoleTopicPlan2);
        plansProvider.createAddressPlanConfig(consoleQueuePlan3);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 0.0, 3.0),
                new AddressSpaceResource("router", 1.0, 5.0),
                new AddressSpaceResource("aggregate", 0.0, 8.0));
        List<AddressPlan> addressPlans = Arrays.asList(consoleQueuePlan1, consoleTopicPlan2, consoleQueuePlan3);
        AddressSpacePlan consolePlan = new AddressSpacePlan("console-plan", "console-plan",
                "standard-space", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlanConfig(consolePlan);

        //create address space plan with new plan
        AddressSpace consoleAddrSpace = new AddressSpace("console-plan-instance", AddressSpaceType.STANDARD,
                consolePlan.getName(), AuthService.STANDARD);
        createAddressSpace(consoleAddrSpace);

        //create new user
        String username = "test_newplan_name";
        String password = "test_newplan_password";
        getKeycloakClient().createUser(consoleAddrSpace.getName(), username, password, 20, TimeUnit.SECONDS);

        //create addresses
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(consoleAddrSpace), addressApiClient, consoleAddrSpace, username, password);
        consoleWebPage.openWebConsolePage();
        Destination q1 = Destination.queue("new-queue-instance-1", consoleQueuePlan1.getName());
        Destination t2 = Destination.topic("new-topic-instance-2", consoleTopicPlan2.getName());
        Destination q3 = Destination.queue("new-queue-instance-3", consoleQueuePlan3.getName());
        consoleWebPage.createAddressesWebConsole(q1, t2, q3);

        String assertMessage = "Address plan wasn't set properly";
        assertEquals(assertMessage, q1.getPlan(), consoleWebPage.getAddressItem(q1).getPlan());
        assertEquals(assertMessage, t2.getPlan(), consoleWebPage.getAddressItem(t2).getPlan());
        assertEquals(assertMessage, q3.getPlan(), consoleWebPage.getAddressItem(q3).getPlan());

        //simple send/receive
        amqpClientFactory = new AmqpClientFactory(kubernetes, environment, consoleAddrSpace, username, password);
        AmqpClient queueClient = amqpClientFactory.createQueueClient(consoleAddrSpace);
        queueClient.getConnectOptions().setUsername(username);
        queueClient.getConnectOptions().setPassword(password);

        AmqpClient topicClient = amqpClientFactory.createTopicClient(consoleAddrSpace);
        topicClient.getConnectOptions().setUsername(username);
        topicClient.getConnectOptions().setPassword(password);

        QueueTest.runQueueTest(queueClient, q1, 333);
        TopicTest.runTopicTest(topicClient, t2, 333);
        QueueTest.runQueueTest(queueClient, q3, 333);

        consoleWebPage.deleteAddressWebConsole(t2); //remaining addresses will be removed automatically via tearDown
    }
}
