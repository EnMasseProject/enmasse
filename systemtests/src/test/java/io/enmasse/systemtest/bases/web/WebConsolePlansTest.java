/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.shared.standard.QueueTest;
import io.enmasse.systemtest.shared.standard.TopicTest;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import org.junit.jupiter.api.AfterEach;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class WebConsolePlansTest extends TestBase implements ITestIsolatedStandard {
    SeleniumProvider selenium = SeleniumProvider.getInstance();
    private ConsoleWebPage consoleWebPage;

    @AfterEach
    public void tearDownDrivers() throws Exception {
        selenium.tearDownDrivers();
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

        isolatedResourcesManager.createAddressPlan(consoleQueuePlan1);
        isolatedResourcesManager.createAddressPlan(consoleTopicPlan2);
        isolatedResourcesManager.createAddressPlan(consoleQueuePlan3);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 3.0),
                new ResourceAllowance("router", 5.0),
                new ResourceAllowance("aggregate", 8.0));
        List<AddressPlan> addressPlans = Arrays.asList(consoleQueuePlan1, consoleTopicPlan2, consoleQueuePlan3);
        AddressSpacePlan consolePlan = PlanUtils.createAddressSpacePlanObject("console-plan",
                "default-minimal", AddressSpaceType.STANDARD, resources, addressPlans);
        isolatedResourcesManager.createAddressSpacePlan(consolePlan);

        //create address space plan with new plan
        AddressSpace consoleAddrSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("console-plan-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(consolePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        isolatedResourcesManager.createAddressSpace(consoleAddrSpace);

        //create new user
        UserCredentials user = new UserCredentials("test-newplan-name", "test_newplan_password");
        isolatedResourcesManager.createOrUpdateUser(consoleAddrSpace, user);

        //create addresses
        consoleWebPage = new ConsoleWebPage(selenium, kubernetes.getConsoleRoute(consoleAddrSpace), consoleAddrSpace, clusterUser);
        consoleWebPage.openWebConsolePage();
        Address q1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(consoleAddrSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(consoleAddrSpace, "new-queue-instance"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("new-queue-instance")
                .withPlan(consoleQueuePlan1.getMetadata().getName())
                .endSpec()
                .build();
        Address t2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(consoleAddrSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(consoleAddrSpace, "new-topic-instance-2"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("new-topic-instance-2")
                .withPlan(consoleTopicPlan2.getMetadata().getName())
                .endSpec()
                .build();
        Address q3 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(consoleAddrSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(consoleAddrSpace, "new-queue-instance-3"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("new-queue-instance-3")
                .withPlan(consoleQueuePlan3.getMetadata().getName())
                .endSpec()
                .build();
        consoleWebPage.createAddressesWebConsole(q1, t2, q3);

        String assertMessage = "Address plan wasn't set properly";
        assertEquals(q1.getSpec().getPlan(), consoleWebPage.getAddressItem(q1).getPlan(), assertMessage);
        assertEquals(t2.getSpec().getPlan(), consoleWebPage.getAddressItem(t2).getPlan(), assertMessage);
        assertEquals(q3.getSpec().getPlan(), consoleWebPage.getAddressItem(q3).getPlan(), assertMessage);

        //simple send/receive
        resourcesManager.setAmqpClientFactory(new AmqpClientFactory(consoleAddrSpace, user));
        AmqpClient queueClient = getAmqpClientFactory().createQueueClient(consoleAddrSpace);
        queueClient.getConnectOptions().setCredentials(user);

        AmqpClient topicClient = getAmqpClientFactory().createTopicClient(consoleAddrSpace);
        topicClient.getConnectOptions().setCredentials(user);

        QueueTest.runQueueTest(queueClient, q1, 333);
        TopicTest.runTopicTest(topicClient, t2, 333);
        QueueTest.runQueueTest(queueClient, q3, 333);

        consoleWebPage.deleteAddressWebConsole(t2); //remaining addresses will be removed automatically via tearDown
    }
}
