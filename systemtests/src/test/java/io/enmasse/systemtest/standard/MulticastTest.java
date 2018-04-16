/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.bases.ITestBaseStandard;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import org.junit.jupiter.api.Test;

public class MulticastTest extends TestBaseWithShared implements ITestBaseStandard {

    @Test
    public void testRestApi() throws Exception {
        Destination m1 = Destination.multicast("multicastRest1");
        Destination m2 = Destination.multicast("multicastRest2");

        runRestApiTest(sharedAddressSpace, m1, m2);
    }
}
