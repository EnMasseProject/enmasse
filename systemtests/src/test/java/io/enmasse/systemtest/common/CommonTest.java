/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.CRDCmdClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(isolated)
class CommonTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();

    @Test
    void testAccessLogs() throws Exception {
        AddressSpace standard = new AddressSpace("standard-addr-space-logs", AddressSpaceType.STANDARD, AuthService.STANDARD);
        createAddressSpace(standard);

        Destination dest = Destination.queue("test-queue", DestinationPlan.STANDARD_SMALL_QUEUE.plan());
        setAddresses(standard, dest);

        kubernetes.listPods().forEach(pod -> {
            kubernetes.getContainersFromPod(pod.getMetadata().getName()).forEach(container -> {
                log.info("Getting log from pod: {}, for container: {}", pod.getMetadata().getName(), container.getName());
                assertFalse(kubernetes.getLog(pod.getMetadata().getName(), container.getName()).isEmpty());
            });
        });
    }

    @Test
    void testRestartComponents() throws Exception {
        List<Label> labels = new LinkedList<>();
        labels.add(new Label("component", "api-server"));
        labels.add(new Label("name", "keycloak"));
        labels.add(new Label("name", "keycloak-controller"));
        labels.add(new Label("name", "address-space-controller"));

        UserCredentials user = new UserCredentials("frantisek", "dobrota");
        AddressSpace standard = new AddressSpace("addr-space-restart-standard", AddressSpaceType.STANDARD, AuthService.STANDARD);
        AddressSpace brokered = new AddressSpace("addr-space-restart-brokered", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpaceList(standard, brokered);
        createUser(brokered, user);
        createUser(standard, user);

        List<Destination> brokeredAddresses = getAllBrokeredAddresses();
        List<Destination> standardAddresses = getAllStandardAddresses();

        setAddresses(brokered, brokeredAddresses.toArray(new Destination[0]));
        setAddresses(standard, standardAddresses.toArray(new Destination[0]));

        assertCanConnect(brokered, user, brokeredAddresses);
        assertCanConnect(standard, user, standardAddresses);

        //number of pods running before restarting any
        int runningPodsBefore = kubernetes.listPods().size();
        log.info("Number of running pods before restarting any: {}", runningPodsBefore);

        for (Label label : labels) {
            log.info("Restarting {}", label.labelValue);
            CRDCmdClient.deletePodByLabel(label.getLabelName(), label.getLabelValue());
            Thread.sleep(30_000);
            TestUtils.waitForExpectedReadyPods(kubernetes, runningPodsBefore, new TimeoutBudget(60, TimeUnit.SECONDS));
            assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses);
        }

        log.info("Restarting whole enmasse");
        CRDCmdClient.deletePodByLabel("app", kubernetes.getEnmasseAppLabel());
        Thread.sleep(120_000);
        TestUtils.waitForExpectedReadyPods(kubernetes, runningPodsBefore, new TimeoutBudget(120, TimeUnit.SECONDS));
        TestUtils.waitForDestinationsReady(addressApiClient, standard, new TimeoutBudget(180, TimeUnit.SECONDS),
                standardAddresses.toArray(new Destination[0]));
        assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses);
    }

    @Test
    void testMonitoringTools() throws Exception {
        AddressSpace standard = new AddressSpace("standard-addr-space-monitor", AddressSpaceType.STANDARD, AuthService.STANDARD);
        createAddressSpace(standard);
        setAddresses(standard, getAllStandardAddresses().toArray(new Destination[0]));

        String qdRouterName = TestUtils.listRunningPods(kubernetes, standard).stream()
                .filter(pod -> pod.getMetadata().getName().contains("qdrouter"))
                .collect(Collectors.toList()).get(0).getMetadata().getName();
        assertTrue(CRDCmdClient.runQDstat(qdRouterName, "-c").getRetCode());
        assertTrue(CRDCmdClient.runQDstat(qdRouterName, "-a").getRetCode());
        assertTrue(CRDCmdClient.runQDstat(qdRouterName, "-l").getRetCode());
    }

    private void assertSystemWorks(AddressSpace brokered, AddressSpace standard, UserCredentials existingUser,
                                   List<Destination> brAddresses, List<Destination> stAddresses) throws Exception {
        log.info("Check if system works");
        brokered = getAddressSpace(brokered.getName());
        standard = getAddressSpace(standard.getName());
        createUser(brokered, new UserCredentials("jenda", "cenda"));
        createUser(standard, new UserCredentials("jura", "fura"));
        assertCanConnect(brokered, existingUser, brAddresses);
        assertCanConnect(standard, existingUser, stAddresses);
    }

    private class Label {
        String labelName;
        String labelValue;

        Label(String labelName, String labelValue) {
            this.labelName = labelName;
            this.labelValue = labelValue;
        }

        String getLabelName() {
            return labelName;
        }

        String getLabelValue() {
            return labelValue;
        }
    }

}

