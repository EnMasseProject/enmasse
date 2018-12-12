/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.*;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class RestartTest extends MarathonTestBase {
    private static Logger log = CustomLogger.getLogger();
    private ScheduledExecutorService deleteService;

    @BeforeEach
    void setUp() {
        deleteService = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void tearDownRestart() throws InterruptedException {
        if (deleteService != null) {
            deleteService.shutdownNow();
        }
    }

    @Test
    void testRandomDeletePods() throws Exception {

        UserCredentials user = new UserCredentials("test-user", "passsswooooord");
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

        //set up restart scheduler
        deleteService.scheduleAtFixedRate(() -> {
            log.info("............................................................");
            log.info("............................................................");
            log.info("..........Scheduler will pick pod and delete them...........");
            List<Pod> pods = kubernetes.listPods();
            int podNum = new Random().nextInt(pods.size() - 1);
            kubernetes.deletePod(environment.namespace(), pods.get(podNum).getMetadata().getName());
            log.info("............................................................");
            log.info("............................................................");
            log.info("............................................................");
        }, 5, 25, TimeUnit.SECONDS);

        runTestInLoop(60, () ->
                assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses));
    }

    private void assertSystemWorks(AddressSpace brokered, AddressSpace standard, UserCredentials existingUser,
                                   List<Destination> brAddresses, List<Destination> stAddresses) throws Exception {
        log.info("Check if system works");
        TestUtils.runUntilPass(60, () -> getAddressSpace(brokered.getName()));
        TestUtils.runUntilPass(60, () -> getAddressSpace(standard.getName()));
        TestUtils.runUntilPass(60, () -> createUser(brokered, new UserCredentials("jenda", "cenda")));
        TestUtils.runUntilPass(60, () -> createUser(standard, new UserCredentials("jura", "fura")));
        TestUtils.runUntilPass(60, () -> { assertCanConnect(brokered, existingUser, brAddresses); return true; });
        TestUtils.runUntilPass(60, () -> { assertCanConnect(standard, existingUser, stAddresses); return true; });
    }
}

