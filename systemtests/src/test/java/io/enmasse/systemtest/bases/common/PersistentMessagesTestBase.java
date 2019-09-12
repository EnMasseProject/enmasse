/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.common;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestBaseShared;
import io.enmasse.systemtest.clients.ClientUtils;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class PersistentMessagesTestBase extends TestBase implements ITestBaseShared {

    private static Logger log = CustomLogger.getLogger();
    private ClientUtils clientUtils = new ClientUtils();

    protected void doTestQueuePersistentMessages(Address address, int messagesBatch) throws Exception {
        resourcesManager.setAddresses(address);

        // Wait for the first console pod to be terminated
        TestUtils.waitForConsoleRollingUpdate(kubernetes.getInfraNamespace());
        TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());

        int podCount = kubernetes.listPods().size();
        clientUtils.sendDurableMessages(resourcesManager, getSharedAddressSpace(), address, defaultCredentials, messagesBatch);
        restartBrokers(podCount);

        // Seems that the service/route can sometimes not be immediately available despite the pod being Ready.
        assertConnectable(getSharedAddressSpace(), defaultCredentials);
        clientUtils.receiveDurableMessages(resourcesManager, getSharedAddressSpace(), address, defaultCredentials, messagesBatch);

    }

    protected void doTestTopicPersistentMessages(Address topic, Address subscription) throws Exception {
        resourcesManager.setAddresses(topic, subscription);

        int podCount = kubernetes.listPods().size();

        AmqpClient client = resourcesManager.getAmqpClientFactory().createTopicClient(getSharedAddressSpace());
        client.getConnectOptions().setCredentials(defaultCredentials);

        log.info("Subscribe first receiver");
        Future<List<Message>> recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription), 30);
        clientUtils.sendDurableMessages(resourcesManager, getSharedAddressSpace(), topic, defaultCredentials, 30);
        assertThat("Wrong messages received: ", recvResults.get(1, TimeUnit.MINUTES).size(), is(30));
        clientUtils.sendDurableMessages(resourcesManager, getSharedAddressSpace(), topic, defaultCredentials, 30);

        restartBrokers(podCount);

        // Seems that the service/route can sometimes not be immediately available despite the pod being Ready.
        assertConnectable(getSharedAddressSpace(), defaultCredentials);

        log.info("Subscribe receiver again");
        recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription), 30);
        assertThat("Wrong messages received: batch2", recvResults.get(1, TimeUnit.MINUTES).size(), is(30));
    }

    private void restartBrokers(int podCount) throws Exception {
        Map<String, String> labelSelector = Collections.singletonMap("role", "broker");
        List<String> uids = kubernetes.listPods(kubernetes.getInfraNamespace(), labelSelector).stream().map(pod -> pod.getMetadata().getUid()).collect(Collectors.toList());

        kubernetes.deletePod(kubernetes.getInfraNamespace(), labelSelector);

        waitForPodsToTerminate(uids);
        TestUtils.waitForExpectedReadyPods(kubernetes, kubernetes.getInfraNamespace(), podCount, new TimeoutBudget(10, TimeUnit.MINUTES));
        log.info("Broker pods restarted");
    }

    private void assertConnectable(AddressSpace space, UserCredentials user) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        String name = space.getMetadata().getName();
        do {
            try {
                clientUtils.connectAddressSpace(resourcesManager, space, user);
                log.info("Successfully connected to address space : {}", name);
                return;
            } catch (IOException e) {
                log.info("Failed to connect to address space: {} - {}", name, e.getMessage());
            }
            Thread.sleep(1000);
        } while (!budget.timeoutExpired());

        fail(String.format("Failed to assert address space %s connectable within timeout", name));
    }

}
