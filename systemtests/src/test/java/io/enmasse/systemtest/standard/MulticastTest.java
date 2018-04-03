/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.bases.StandardTestBase;
import org.junit.Test;

public class MulticastTest extends StandardTestBase {

    @Test
    public void testRestApi() throws Exception {
        Destination m1 = Destination.multicast("multicastRest1");
        Destination m2 = Destination.multicast("multicastRest2");

        runRestApiTest(m1, m2);
    }
}
