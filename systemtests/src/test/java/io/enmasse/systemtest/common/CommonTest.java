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

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

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
    void testRestartContainersByLabel() throws Exception {
        UserCredentials user = new UserCredentials("admin", "admin");
        List<String> compLabel = Arrays.asList("api-server");
        List<String> nameLabel = Arrays.asList("keycloak", "keycloak-controller", "address-space-controller");
        AddressSpace standard = new AddressSpace("none-addr-space-restart", AddressSpaceType.STANDARD, AuthService.STANDARD);

        //create addr space and addresses
        createAddressSpace(standard);
        Destination queue = new Destination("test-queue", Destination.QUEUE, "pooled-queue");
        setAddresses(standard, queue);

        //number of pods running before restarting any
        int runningPodsBefore = TestUtils.listRunningPods(kubernetes, standard).size();
        log.info("Number of running pods before restarting any: {}", runningPodsBefore);  //this is a wrong result (3)?

        nameLabel.stream().forEach(label -> {
            CRDCmdClient.deletePodbyName(label);
            //sleep after restarting pod and assert no connection possible
            try {
                assertCannotConnect(standard, user, Arrays.asList(queue));
                Thread.sleep(20);    //not best to time it, need another way
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                assertCanConnect(standard, user, Arrays.asList(queue));
            } catch (Exception e) {
                log.warn("Exception happened here ( can connect ) ");
            }

        });

        //restart each pod in turn ( by label=can be more than pod )
        compLabel.stream().forEach(label -> {
            CRDCmdClient.deletePodByCompenent(label);

            //sleep after restarting pod
            try {
                log.info("************   assertCannotConnect   COMPLABEL  *****************");
                assertCannotConnect(standard, user, Arrays.asList(queue));
                Thread.sleep(30);
            } catch (Exception e) {
                //e.printStackTrace();
                log.warn("Exception happened here ( cannot connect )");
            }

            //number of pods running after restarting one by label
            int runningPodsAfter = TestUtils.listRunningPods(kubernetes, standard).size();
            assertTrue(runningPodsAfter == runningPodsBefore, "Problem with restarting pod");

            //try to connect to enmasse after each pod restart
            try {
                log.info("************   assertCanConnect  COMPLABEL   *****************");
                assertCanConnect(standard, user, Arrays.asList(queue));
            } catch (Exception e) {
                //e.printStackTrace();
                log.warn("Exception happened here ( can connect ) ");
            }
        });
        deleteAddressSpace(standard);
    }

//use in built monitoring tools to see whats going on with enmasse
//        cfeate addr space
//use qdstat and ( may need installing , check docs ) to see if all router metrics are exposed ( check whats  in docs )
// qdstat should run from inside qdrouter-* pod
    @Test
    void testMonitoringTools() throws Exception {
        UserCredentials user = new UserCredentials("developer", "developer");
        AddressSpace standard = new AddressSpace("standard-addr-space-monitor", AddressSpaceType.STANDARD, AuthService.STANDARD);
        createAddressSpace(standard);

        Destination anycast = new Destination("test-queue-before", null, standard.getName(),
                "addr_1", AddressType.QUEUE.toString(), "pooled-queue");
        addressApiClient.createAddress(anycast);

        //not sure how to use executor to use qdsat from within qdrouter- pod
    }

}

