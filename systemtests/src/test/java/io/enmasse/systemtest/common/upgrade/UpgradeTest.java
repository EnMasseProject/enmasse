/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.upgrade;


import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.CmdClient;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.upgrade;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(upgrade)
class UpgradeTest extends TestBase {

    private static Logger log = CustomLogger.getLogger();

    @Test
    void testFunctionalityBeforeAndAfterUpgrade() throws Exception {
        runUpgradeTest(false);
        applyEnmasseVersion(Paths.get(Environment.getInstance().getUpgradeTemplates()), false);
        runUpgradeTest(true);
        applyEnmasseVersion(Paths.get(Environment.getInstance().getDowngradeTemplates()), true);
        runUpgradeTest(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Help methods
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private void applyEnmasseVersion(Path templatePaths, boolean downgrade) throws Exception {
        Path inventoryFile = Paths.get(System.getProperty("user.dir"), "ansible", "inventory", "systemtests.inventory");
        Path ansiblePlaybook = Paths.get(templatePaths.toString(), "ansible", "playbooks", "openshift", "deploy_all.yml");
        List<String> cmd = Arrays.asList("ansible-playbook", ansiblePlaybook.toString(), "-i", inventoryFile.toString(),
                "--extra-vars", String.format("namespace=%s", kubernetes.getNamespace()));

        assertTrue(CmdClient.execute(cmd, 300_000, true).getRetCode(), "Deployment of new version of enmasse failed");
        log.info("Sleep after {}", downgrade ? "downgrade" : "upgrade");
        Thread.sleep(300_000);

        checkImagesUpdated(templatePaths.getFileName().toString().split("-")[1]);
    }

    private void checkImagesUpdated(String version) throws Exception {
        Path makefileDir = Paths.get(System.getProperty("user.dir"), "..");
        Path imageEnvDir = Paths.get(makefileDir.toString(), "imageenv.txt");

        CmdClient.execute(Arrays.asList("make", "-C", makefileDir.toString(), "TAG=" + version, "imageenv"), 10_000, false);
        String images = Files.readString(imageEnvDir);
        log.info("Expected images: {}", images);

        TestUtils.waitUntilCondition("Images are updated", () -> {
            AtomicBoolean ready = new AtomicBoolean(true);
            log.info("=======================================================");
            kubernetes.listPods().forEach(pod -> {
                pod.getSpec().getContainers().forEach(container -> {
                    log.info("Pod {}, current container {}", pod.getMetadata().getName(), container.getImage());
                    if (!images.contains(container.getImage().replace("enmasse-controller-manager", "controller-manager"))) { //TODO workaround due to image rename
                        log.warn("Container is not upgraded");
                        ready.set(false);
                    } else {
                        log.info("Container is upgraded");
                    }
                });
                pod.getSpec().getInitContainers().forEach(initContainer -> {
                    log.info("Pod {}, current initContainer {}", pod.getMetadata().getName(), initContainer.getImage());
                    if (!images.contains(initContainer.getImage())) {
                        log.warn("Init container is not upgraded");
                        ready.set(false);
                    } else {
                        log.info("InitContainer is upgraded");
                    }
                });
                log.info("*********************************************");
            });
            return ready.get();
        }, new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    private void runUpgradeTest(boolean upgraded) throws Exception {
        AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("brokered-addr-space", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD);
        AddressSpace standard = AddressSpaceUtils.createAddressSpaceObject("standard-addr-space", AddressSpaceType.STANDARD, AuthenticationServiceType.STANDARD);
        List<Address> standardAddresses = getAllStandardAddresses();
        List<Address> brokeredAddresses = getAllBrokeredAddresses();

        List<Address> brokeredQueues = getQueues(brokeredAddresses);
        List<Address> standardQueues = getQueues(standardAddresses);

        UserCredentials cred = new UserCredentials("kornelius", "korny");
        int msgCount = 13;

        if (!upgraded) {
            log.info("Before upgrade phase");
            createAddressSpaceList(brokered, standard);

            createUser(brokered, cred);
            createUser(standard, cred);

            setAddresses(brokered, brokeredAddresses.toArray(new Address[0]));
            setAddresses(standard, standardAddresses.toArray(new Address[0]));

            assertCanConnect(brokered, cred, brokeredAddresses);
            assertCanConnect(standard, cred, standardAddresses);

            log.info("Send durable messages to brokered queue");
            for (Address dest : brokeredQueues) {
                sendDurableMessages(brokered, dest, cred, msgCount);
            }
            log.info("Send durable messages to standard queues");
            for (Address dest : standardQueues) {
                sendDurableMessages(standard, dest, cred, msgCount);
            }
            Thread.sleep(10_000);
            log.info("End of before upgrade phase");
        } else {
            log.info("After upgrade phase");

            brokered = getAddressSpace(brokered.getMetadata().getName());
            standard = getAddressSpace(standard.getMetadata().getName());

            waitForAddressSpaceReady(brokered);
            waitForAddressSpaceReady(standard);

            Thread.sleep(120_000);

            log.info("Receive durable messages from brokered queue");
            for (Address dest : brokeredQueues) {
                receiveDurableMessages(brokered, dest, cred, msgCount);
            }
            log.info("Receive durable messages from standard queues");
            for (Address dest : standardQueues) {
                receiveDurableMessages(standard, dest, cred, msgCount);
            }

            assertCanConnect(brokered, cred, brokeredAddresses);
            assertCanConnect(standard, cred, standardAddresses);

            log.info("End of after upgrade phase");

            log.info("Send durable messages to brokered queue");
            for (Address dest : brokeredQueues) {
                sendDurableMessages(brokered, dest, cred, msgCount);
            }
            log.info("Send durable messages to standard queues");
            for (Address dest : standardQueues) {
                sendDurableMessages(standard, dest, cred, msgCount);
            }
        }
    }

    private List<Address> getQueues(List<Address> addresses) {
        return addresses.stream().filter(dest -> dest.getSpec().getType()
                .equals(AddressType.QUEUE.toString())).collect(Collectors.toList());
    }
}
