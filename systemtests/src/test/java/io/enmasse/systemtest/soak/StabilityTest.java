/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.soak;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.condition.MultinodeCluster;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.operator.EnmasseOperatorManager;
import io.enmasse.systemtest.platform.cluster.KubeClusterManager;
import io.enmasse.systemtest.annotations.SeleniumFirefox;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Node;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;

import static io.enmasse.systemtest.TestTag.SOAK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag(SOAK)
class StabilityTest extends TestBase implements ITestBaseIsolated {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private final KubeClusterManager cm = KubeClusterManager.getInstance();

    /**
     * Test for check if enmasse survive kube cluster node drain (pod mogration)
     */
    @Test
    @SeleniumFirefox
    @OpenShift(version = OpenShiftVersion.OCP4, multinode = MultinodeCluster.YES)
    void testUsabilityAfterNodeDrain() throws Exception {
        int durableMessagesCount = 100;
        UserCredentials user = new UserCredentials("user", "passwd");
        List<Node> workerNodes = cm.getWorkerNodes();
        ConsoleWebPage console = new ConsoleWebPage(SeleniumProvider.getInstance(), TestUtils.getGlobalConsoleRoute(), clusterUser);

        //------------------------------------------------
        // create infra
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("space-brokered")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("space-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(standard, brokered);
        resourcesManager.createOrUpdateUser(brokered, user);
        resourcesManager.createOrUpdateUser(standard, user);

        List<Address> brokeredAddresses = AddressUtils.getAllBrokeredAddresses(brokered);
        List<Address> standardAddresses = AddressUtils.getAllStandardAddresses(standard);
        Address[] brokerQueue = brokeredAddresses.stream().filter(address -> address.getSpec().getType().equals(AddressType.QUEUE.toString())).toArray(Address[]::new);
        Address[] standardQueue = standardAddresses.stream().filter(address -> address.getSpec().getType().equals(AddressType.QUEUE.toString())).toArray(Address[]::new);

        resourcesManager.setAddresses(brokeredAddresses.toArray(new Address[0]));
        resourcesManager.setAddresses(standardAddresses.toArray(new Address[0]));

        //------------------------------------------------
        // drain nodes
        for (Node workerNode : workerNodes) {
            //send durable messages
            getClientUtils().sendDurableMessages(resourcesManager, brokered, user, durableMessagesCount, brokerQueue);
            getClientUtils().sendDurableMessages(resourcesManager, standard, user, durableMessagesCount, standardQueue);

            //drain oopenshift node
            cm.drainNode(workerNode.getMetadata().getName());

            //wait until infra is ready
            EnmasseOperatorManager.getInstance().waitUntilOperatorReady(kubernetes.getInfraNamespace());

            kubernetes.listAllPods(kubernetes.getInfraNamespace()).forEach(pod ->
                    assertNotEquals(workerNode.getMetadata().getName(), pod.getSpec().getHostname(),
                            String.format("Pod is not drained from %s", pod.getMetadata().getName())));

            //receive durable messages
            getClientUtils().receiveDurableMessages(resourcesManager, brokered, user, durableMessagesCount, brokerQueue);
            getClientUtils().receiveDurableMessages(resourcesManager, standard, user, durableMessagesCount, standardQueue);

            //check if all addresses are reachable
            getClientUtils().assertCanConnect(brokered, user, brokeredAddresses, resourcesManager);
            getClientUtils().assertCanConnect(standard, user, standardAddresses, resourcesManager);

            //check console is operable
            SeleniumProvider.getInstance().setupDriver(TestUtils.getFirefoxDriver()); //connect again because selenium pod can be restarted due to node drain
            console.openConsolePage();
            assertEquals(2, console.getAddressSpaceItems().size(),
                    "Console after node drain does not show all addressspaces");

            //set schedule of node back
            cm.setNodeSchedule(workerNode.getMetadata().getName(), true);
        }
    }
}

