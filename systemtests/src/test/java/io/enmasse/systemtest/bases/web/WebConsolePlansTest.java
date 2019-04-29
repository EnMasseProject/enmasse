/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.AdminResourcesManager;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.standard.TopicTest;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import java.util.Arrays;
import java.util.List;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(isolated)
public abstract class WebConsolePlansTest extends TestBase implements ISeleniumProvider {

    private static final AdminResourcesManager adminManager = new AdminResourcesManager();

    private ConsoleWebPage consoleWebPage;

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        selenium.setupDriver(environment, kubernetes, buildDriver());
        adminManager.setUp();
    }

    @AfterEach
    public void tearDownDrivers() throws Exception {
        selenium.tearDownDrivers();
        adminManager.tearDown();
    }

    //============================================================================================
    //============================ do test methods ===============================================
    //============================================================================================

    /**
     * related github issue: #921
     */
    protected void doTestCreateAddressPlan() throws Exception {
        //define and create address plans
        List<ResourceRequest> addressResourcesQueue1 = Arrays.asList(new ResourceRequest("broker", 0.15), new ResourceRequest("router", 0.0));
        List<ResourceRequest> addressResourcesTopic2 = Arrays.asList(
                new ResourceRequest("broker", 0.3),
                new ResourceRequest("router", 0.2));
        List<ResourceRequest> addressResourcesQueue3 = Arrays.asList(new ResourceRequest("broker", 0.25), new ResourceRequest("router", 0.0));
        AddressPlan consoleQueuePlan1 = PlanUtils.createAddressPlanObject("console-queue-1", AddressType.QUEUE, addressResourcesQueue1);
        AddressPlan consoleTopicPlan2 = PlanUtils.createAddressPlanObject("console-topic-2", AddressType.TOPIC, addressResourcesTopic2);
        AddressPlan consoleQueuePlan3 = PlanUtils.createAddressPlanObject("console-queue-3", AddressType.QUEUE, addressResourcesQueue3);

        adminManager.createAddressPlan(consoleQueuePlan1);
        adminManager.createAddressPlan(consoleTopicPlan2);
        adminManager.createAddressPlan(consoleQueuePlan3);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 3.0),
                new ResourceAllowance("router", 5.0),
                new ResourceAllowance("aggregate", 8.0));
        List<AddressPlan> addressPlans = Arrays.asList(consoleQueuePlan1, consoleTopicPlan2, consoleQueuePlan3);
        AddressSpacePlan consolePlan = PlanUtils.createAddressSpacePlanObject("console-plan",
                "default-with-mqtt", AddressSpaceType.STANDARD, resources, addressPlans);
        adminManager.createAddressSpacePlan(consolePlan);

        //create address space plan with new plan
        AddressSpace consoleAddrSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("console-plan-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString().toLowerCase())
                .withPlan(consolePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(consoleAddrSpace);

        //create new user
        UserCredentials user = new UserCredentials("test-newplan-name", "test_newplan_password");
        createUser(consoleAddrSpace, user);

        //create addresses
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(consoleAddrSpace), consoleAddrSpace, clusterUser);
        consoleWebPage.openWebConsolePage();
        Address q1 = AddressUtils.createQueueAddressObject("new-queue-instance-1", consoleQueuePlan1.getMetadata().getName());
        Address t2 = AddressUtils.createTopicAddressObject("new-topic-instance-2", consoleTopicPlan2.getMetadata().getName());
        Address q3 = AddressUtils.createQueueAddressObject("new-queue-instance-3", consoleQueuePlan3.getMetadata().getName());
        consoleWebPage.createAddressesWebConsole(q1, t2, q3);

        String assertMessage = "Address plan wasn't set properly";
        assertEquals(q1.getSpec().getPlan(), consoleWebPage.getAddressItem(q1).getPlan(), assertMessage);
        assertEquals(t2.getSpec().getPlan(), consoleWebPage.getAddressItem(t2).getPlan(), assertMessage);
        assertEquals(q3.getSpec().getPlan(), consoleWebPage.getAddressItem(q3).getPlan(), assertMessage);

        //simple send/receive
        amqpClientFactory = new AmqpClientFactory(consoleAddrSpace, user);
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
