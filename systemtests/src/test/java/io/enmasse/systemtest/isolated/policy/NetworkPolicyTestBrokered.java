/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.policy;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanBuilder;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.BrokeredInfraConfigBuilder;
import io.enmasse.admin.model.v1.BrokeredInfraConfigSpecAdminBuilder;
import io.enmasse.admin.model.v1.BrokeredInfraConfigSpecBrokerBuilder;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedBrokered;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.MessagingUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyIngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyPeer;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyPeerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@OpenShift(version = 4)
class NetworkPolicyTestBrokered extends TestBase implements ITestIsolatedBrokered {
    private UserCredentials credentials = new UserCredentials("test", "test");
    private String blockedSpace = "blocked-namespace";
    private String allowedSpace = "allowed-namespace";

    @AfterEach
    void clearNamespaces() throws Exception {
        KUBERNETES.deleteNamespace(blockedSpace);
        TestUtils.waitForNamespaceDeleted(KUBERNETES, blockedSpace);
        KUBERNETES.deleteNamespace(allowedSpace);
        TestUtils.waitForNamespaceDeleted(KUBERNETES, allowedSpace);
    }

    @Test
    void testNetworkPolicyWithPodSelector() throws Exception {
        int expectedMsgCount = 5;
        HashMap<String, String> map = new HashMap<>();
        map.put("app", allowedSpace);

        LabelSelector pod = new LabelSelectorBuilder()
                .withMatchLabels(map)
                .build();
        NetworkPolicyPeer networkPolicyPeer = new NetworkPolicyPeerBuilder()
                .withPodSelector(pod)
                .build();

        BrokeredInfraConfig standardInfraConfig = prepareConfig(networkPolicyPeer);
        AddressPlan addressPlan = prepareAddressPlan();
        AddressSpacePlan addressSpacePlan = prepareAddressSpacePlan(standardInfraConfig, addressPlan);
        AddressSpace addressSpace = prepareAddressSpace(addressSpacePlan);
        Address dest = prepareAddress(addressSpace, addressPlan);

        SystemtestsKubernetesApps.deployMessagingClientApp(blockedSpace);
        SystemtestsKubernetesApps.deployMessagingClientApp(allowedSpace);

        RheaClientSender allowedClientSender = new RheaClientSender(allowedSpace);
        RheaClientReceiver allowedClientReceiver = new RheaClientReceiver(allowedSpace);

        MessagingUtils.preparePolicyClients(allowedClientSender, allowedClientReceiver, dest, addressSpace);

        assertTrue(allowedClientSender.run(), "Sender failed, expected return code 0");
        assertTrue(allowedClientReceiver.run(), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, allowedClientSender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, allowedClientReceiver.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));

        RheaClientSender blockedClientSender = new RheaClientSender(blockedSpace);
        RheaClientReceiver blockedClientReceiver = new RheaClientReceiver(blockedSpace);

        assertFalse(blockedClientSender.run(), "Sender was successful, expected return code -1");
        assertFalse(blockedClientReceiver.run(), "Receiver was successful, expected return code -1");

    }

    @Test
    void testNetworkPolicyWithNamespaceSelector() throws Exception {
        int expectedMsgCount = 5;

        LabelSelector namespace = new LabelSelectorBuilder()
                .withMatchLabels(Collections.singletonMap("allowed", "true"))
                .build();
        NetworkPolicyPeer networkPolicyPeer = new NetworkPolicyPeerBuilder()
                .withNamespaceSelector(namespace)
                .build();

        BrokeredInfraConfig standardInfraConfig = prepareConfig(networkPolicyPeer);
        AddressPlan addressPlan = prepareAddressPlan();
        AddressSpacePlan addressSpacePlan = prepareAddressSpacePlan(standardInfraConfig, addressPlan);
        AddressSpace addressSpace = prepareAddressSpace(addressSpacePlan);
        Address dest = prepareAddress(addressSpace, addressPlan);

        SystemtestsKubernetesApps.deployMessagingClientApp(blockedSpace);
        SystemtestsKubernetesApps.deployMessagingClientApp(allowedSpace);

        RheaClientSender allowedClientSender = new RheaClientSender(allowedSpace);
        RheaClientReceiver allowedClientReceiver = new RheaClientReceiver(allowedSpace);

        MessagingUtils.preparePolicyClients(allowedClientSender, allowedClientReceiver, dest, addressSpace);

        assertTrue(allowedClientSender.run(), "Sender failed, expected return code 0");
        assertTrue(allowedClientReceiver.run(), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, allowedClientSender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, allowedClientReceiver.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));

        RheaClientSender blockedClientSender = new RheaClientSender(blockedSpace);
        RheaClientReceiver blockedClientReceiver = new RheaClientReceiver(blockedSpace);

        assertFalse(blockedClientSender.run(), "Sender was successful, expected return code -1");
        assertFalse(blockedClientReceiver.run(), "Receiver was successful, expected return code -1");

    }

    private BrokeredInfraConfig prepareConfig(NetworkPolicyPeer networkPolicyPeer) {

        PodTemplateSpec brokerTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "broker"), "mybrokernode", "broker");
        PodTemplateSpec adminTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "admin"), "myadminnode", "admin");
        BrokeredInfraConfig brokeredInfraConfig = new BrokeredInfraConfigBuilder()
                .withNewMetadata()
                .withName("test-infra-3-brokered")
                .endMetadata()
                .withNewSpec()
                .withNewNetworkPolicy()
                .withIngress(
                        new NetworkPolicyIngressRuleBuilder()
                                .withFrom(networkPolicyPeer)
                                .withPorts()
                                .build()
                )
                .endNetworkPolicy()
                .withVersion(Environment.getInstance().enmasseVersion())
                .withBroker(new BrokeredInfraConfigSpecBrokerBuilder()
                        .withAddressFullPolicy("FAIL")
                        .withNewResources()
                        .withMemory("512Mi")
                        .withStorage("1Gi")
                        .endResources()
                        .withPodTemplate(brokerTemplateSpec)
                        .build())
                .withAdmin(new BrokeredInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory("512Mi")
                        .endResources()
                        .withPodTemplate(adminTemplateSpec)
                        .build())
                .endSpec()
                .build();
        ISOLATED_RESOURCES_MANAGER.createInfraConfig(brokeredInfraConfig);
        return brokeredInfraConfig;
    }

    private AddressSpacePlan prepareAddressSpacePlan(BrokeredInfraConfig brokeredInfraConfig, AddressPlan addressPlan) throws Exception {
        AddressSpacePlan exampleSpacePlan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("example-space-plan-brokered")
                .withNamespace(KUBERNETES.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(AddressSpaceType.BROKERED.toString())
                .withShortDescription("Custom systemtests defined address space plan")
                .withInfraConfigRef(brokeredInfraConfig.getMetadata().getName())
                .withResourceLimits(Stream.of(new ResourceAllowance("broker", 3.0))
                        .collect(Collectors.toMap(ResourceAllowance::getName, ResourceAllowance::getMax)))
                .withAddressPlans(Stream.of(addressPlan).map(addressPlan1 -> addressPlan1.getMetadata().getName())
                        .collect(Collectors.toList()))
                .endSpec()
                .build();
        ISOLATED_RESOURCES_MANAGER.createAddressSpacePlan(exampleSpacePlan);
        return exampleSpacePlan;
    }

    private AddressPlan prepareAddressPlan() throws Exception {
        AddressPlan exampleAddressPlan = PlanUtils.createAddressPlanObject("example-queue-plan-brokered", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 1.0)));

        ISOLATED_RESOURCES_MANAGER.createAddressPlan(exampleAddressPlan);
        return exampleAddressPlan;
    }

    private AddressSpace prepareAddressSpace(AddressSpacePlan addressSpacePlan) throws Exception {
        AddressSpace exampleAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("brokered-address-space")
                .withNamespace(KUBERNETES.getInfraNamespace())
                .withLabels(Collections.singletonMap("allowed", "true"))
                .endMetadata()
                .withNewSpec()
                .withType(getAddressSpaceType().toString())
                .withPlan(addressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        ISOLATED_RESOURCES_MANAGER.createAddressSpace(exampleAddressSpace);
        ISOLATED_RESOURCES_MANAGER.createOrUpdateUser(exampleAddressSpace, credentials);
        return exampleAddressSpace;
    }

    private Address prepareAddress(AddressSpace addressSpace, AddressPlan addressPlan) throws Exception {
        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-basic-policy"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("messageBasicPolicy")
                .withPlan(addressPlan.getMetadata().getName())
                .endSpec()
                .build();

        ISOLATED_RESOURCES_MANAGER.setAddresses(dest);
        return dest;
    }
}
