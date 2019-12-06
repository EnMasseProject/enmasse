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
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecAdminBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecBrokerBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecRouterBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.clients.ClientUtils;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.AddressUtils;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@OpenShift(version = 4)
class NetworkPolicyTestStandard extends TestBase implements ITestIsolatedStandard {


    private UserCredentials credentials = new UserCredentials("test", "test");
    private String blockedSpace = "blocked-namespace";
    private String allowedSpace = "allowed-namespace";

    @AfterEach
    void clearNamespaces() throws Exception {
        kubernetes.deleteNamespace(blockedSpace);
        TestUtils.waitForNamespaceDeleted(kubernetes, blockedSpace);
        kubernetes.deleteNamespace(allowedSpace);
        TestUtils.waitForNamespaceDeleted(kubernetes, allowedSpace);
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

        StandardInfraConfig standardInfraConfig = prepareConfig(networkPolicyPeer);
        AddressPlan addressPlan = prepareAddressPlan();
        AddressSpacePlan addressSpacePlan = prepareAddressSpacePlan(standardInfraConfig, addressPlan);
        AddressSpace addressSpace = prepareAddressSpace(addressSpacePlan);
        Address dest = prepareAddress(addressSpace, addressPlan);

        SystemtestsKubernetesApps.deployMessagingClientApp(blockedSpace);
        SystemtestsKubernetesApps.deployMessagingClientApp(allowedSpace);

        RheaClientSender allowedClientSender = new RheaClientSender(allowedSpace);
        RheaClientReceiver allowedClientReceiver = new RheaClientReceiver(allowedSpace);

        ClientUtils.preparePolicyClients(allowedClientSender, allowedClientReceiver, dest, addressSpace);

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

        deleteAddressSpace(addressSpace);
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

        StandardInfraConfig standardInfraConfig = prepareConfig(networkPolicyPeer);
        AddressPlan addressPlan = prepareAddressPlan();
        AddressSpacePlan addressSpacePlan = prepareAddressSpacePlan(standardInfraConfig, addressPlan);
        AddressSpace addressSpace = prepareAddressSpace(addressSpacePlan);
        Address dest = prepareAddress(addressSpace, addressPlan);

        SystemtestsKubernetesApps.deployMessagingClientApp(blockedSpace);
        SystemtestsKubernetesApps.deployMessagingClientApp(allowedSpace);

        RheaClientSender allowedClientSender = new RheaClientSender(allowedSpace);
        RheaClientReceiver allowedClientReceiver = new RheaClientReceiver(allowedSpace);

        ClientUtils.preparePolicyClients(allowedClientSender, allowedClientReceiver, dest, addressSpace);

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

        deleteAddressSpace(addressSpace);
    }

    private void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        isolatedResourcesManager.deleteAddressSpace(addressSpace);
        assertTrue(Kubernetes.getInstance().getClient().network().networkPolicies().withLabel(addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID)).list().getItems().isEmpty());
    }

    private StandardInfraConfig prepareConfig(NetworkPolicyPeer networkPolicyPeer) {

        PodTemplateSpec brokerTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "broker"), "mybrokernode", "broker");
        PodTemplateSpec adminTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "admin"), "myadminnode", "admin");
        PodTemplateSpec routerTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "router"), "myrouternode", "router");
        StandardInfraConfig standardInfraConfig = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("test-network-policy-infra")
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
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
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withAddressFullPolicy("FAIL")
                        .withNewResources()
                        .withMemory("512Mi")
                        .withStorage("1Gi")
                        .endResources()
                        .withPodTemplate(brokerTemplateSpec)
                        .build())
                .withRouter(new StandardInfraConfigSpecRouterBuilder()
                        .withNewResources()
                        .withMemory("256Mi")
                        .endResources()
                        .withPodTemplate(routerTemplateSpec)
                        .build())
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory("512Mi")
                        .endResources()
                        .withPodTemplate(adminTemplateSpec)
                        .build())
                .endSpec()
                .build();
        isolatedResourcesManager.createInfraConfig(standardInfraConfig);
        return standardInfraConfig;
    }

    private AddressSpacePlan prepareAddressSpacePlan(StandardInfraConfig standardInfraConfig, AddressPlan addressPlan) throws Exception {
        AddressSpacePlan exampleSpacePlan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("testinomino")
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(AddressSpaceType.STANDARD.toString())
                .withShortDescription("Custom systemtests defined address space plan")
                .withInfraConfigRef(standardInfraConfig.getMetadata().getName())
                .withResourceLimits(Arrays.asList(
                        new ResourceAllowance("broker", 3.0),
                        new ResourceAllowance("router", 3.0),
                        new ResourceAllowance("aggregate", 5.0))
                        .stream().collect(Collectors.toMap(ResourceAllowance::getName, ResourceAllowance::getMax)))
                .withAddressPlans(Collections.singletonList(addressPlan)
                        .stream().map(addressPlan1 -> addressPlan1.getMetadata().getName()).collect(Collectors.toList()))
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpacePlan(exampleSpacePlan);
        return exampleSpacePlan;
    }

    private AddressPlan prepareAddressPlan() throws Exception {
        AddressPlan exampleAddressPlan = PlanUtils.createAddressPlanObject("example-queue-plan-standard", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 1.0)));

        isolatedResourcesManager.createAddressPlan(exampleAddressPlan);
        return exampleAddressPlan;
    }

    private AddressSpace prepareAddressSpace(AddressSpacePlan addressSpacePlan) throws Exception {
        AddressSpace exampleAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("example-address-space")
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .withLabels(Collections.singletonMap("allowed", "true"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(addressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        isolatedResourcesManager.createAddressSpace(exampleAddressSpace);
        isolatedResourcesManager.createOrUpdateUser(exampleAddressSpace, credentials);
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

        isolatedResourcesManager.setAddresses(dest);
        return dest;
    }
}
