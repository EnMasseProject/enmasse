package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.StandardTestBase;
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
