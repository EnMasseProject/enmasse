/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TestTag.smoke;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(sharedIot)
@Tag(smoke)
public class HelloWorldTest {

    /**
     * A test which simply tests the existence of IoT tests
     */
    @Test
    public void helloWorld () {
    }
}
