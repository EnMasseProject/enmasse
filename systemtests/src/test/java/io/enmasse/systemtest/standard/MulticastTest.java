/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.bases.StandardTestBase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class MulticastTest extends StandardTestBase {

    @Test
    public void testRestApi() throws Exception {
        List<String> addresses = Arrays.asList("multicastRest1", "multicastRest2");
        Destination m1 = Destination.multicast(addresses.get(0));
        Destination m2 = Destination.multicast(addresses.get(1));

        runRestApiTest(addresses, m1, m2);
    }
}
