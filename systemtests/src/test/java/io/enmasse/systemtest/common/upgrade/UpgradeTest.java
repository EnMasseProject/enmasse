/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.upgrade;


import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.CmdClient;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
                "--extra-vars", String.format("namespace=%s", kubernetes.getInfraNamespace()));

        assertTrue(CmdClient.execute(cmd, 300_000, true).getRetCode(), "Deployment of new version of enmasse failed");
        log.info("Sleep after {}", downgrade ? "downgrade" : "upgrade");
        Thread.sleep(120_000);

        Matcher m = Pattern.compile("-(.*)").matcher(templatePaths.getFileName().toString());
        m.find();
        checkImagesUpdated(m.group().substring(1));
    }

    private void checkImagesUpdated(String version) throws Exception {
        Path makefileDir = Paths.get(System.getProperty("user.dir"), "..");
        Path imageEnvDir = Paths.get(makefileDir.toString(), "imageenv.txt");

        CmdClient.execute(Arrays.asList("make", "-C", makefileDir.toString(), "TAG=" + version, "imageenv"), 10_000, false);
        String images = Files.readString(imageEnvDir);
        log.info("Expected images: {}", images);

        TestUtils.waitUntilCondition("Images are updated", (phase) -> {
            AtomicBoolean ready = new AtomicBoolean(true);
            log.info("=======================================================");
            kubernetes.listPods().forEach(pod -> {
                pod.getSpec().getContainers().forEach(container -> {
                    log.info("Pod {}, current container {}", pod.getMetadata().getName(), container.getImage());
                    if (!images.contains(container.getImage()
                            .replaceAll("^.*/", "")
                            .replace("enmasse-controller-manager", "controller-manager"))) { //TODO workaround due to image rename (remove after 0.28 release)
                        log.warn("Container is not upgraded");
                        ready.set(false);
                    } else {
                        log.info("Container is upgraded");
                    }
                });
                pod.getSpec().getInitContainers().forEach(initContainer -> {
                    log.info("Pod {}, current initContainer {}", pod.getMetadata().getName(), initContainer.getImage());
                    if (!images.contains(initContainer.getImage()
                            .replaceAll("^.*/", ""))) {
                        log.warn("Init container is not upgraded");
                        ready.set(false);
                    } else {
                        log.info("InitContainer is upgraded");
                    }
                });
                log.info("*********************************************");
            });
            return ready.get();
        }, new TimeoutBudget(10, TimeUnit.MINUTES));
    }

    private void runUpgradeTest(boolean upgraded) throws Exception {
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("brokered-addr-space")
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
                .withName("standard-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED_WITH_MQTT)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        List<Address> standardAddresses = getAllStandardAddresses(standard);
        List<Address> brokeredAddresses = getAllBrokeredAddresses(brokered);

        List<Address> brokeredQueues = getQueues(brokeredAddresses);
        List<Address> standardQueues = getQueues(standardAddresses);

        UserCredentials cred = new UserCredentials("kornelius", "korny");
        int msgCount = 13;

        if (!upgraded) {
            log.info("Before upgrade phase");
            createAddressSpaceList(brokered, standard);

            createOrUpdateUser(brokered, cred);
            createOrUpdateUser(standard, cred);

            setAddresses(brokeredAddresses.toArray(new Address[0]));
            setAddresses(standardAddresses.toArray(new Address[0]));

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
