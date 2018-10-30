/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
