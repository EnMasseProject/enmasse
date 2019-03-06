/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.upgrade;


import io.enmasse.address.model.Address;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.upgrade;

@Tag(upgrade)
class UpgradeTest extends TestBase {

    private static Logger log = CustomLogger.getLogger();

    @Test
    void testFunctionalityBeforeAndAfterUpgrade() throws Exception {
        AddressSpace brokered = new AddressSpace("brokered-addr-space", AddressSpaceType.BROKERED, AuthService.STANDARD);
        AddressSpace standard = new AddressSpace("standard-addr-space", AddressSpaceType.STANDARD, AuthService.STANDARD);
        List<Address> standardAddresses = getAllStandardAddresses();
        List<Address> brokeredAddresses = getAllBrokeredAddresses();

        List<Address> brokeredQueues = getQueues(brokeredAddresses);
        List<Address> standardQueues = getQueues(standardAddresses);

        UserCredentials cred = new UserCredentials("kornelius", "korny");
        int msgCount = 13;

        if (!environment.isUpgraded()) {
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

            brokered = getAddressSpace(brokered.getName());
            standard = getAddressSpace(standard.getName());

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
