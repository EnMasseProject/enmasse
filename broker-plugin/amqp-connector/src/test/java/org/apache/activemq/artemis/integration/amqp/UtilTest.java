/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package org.apache.activemq.artemis.integration.amqp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class UtilTest {

    @Test
    public void parseIntOrNullWorksWithStreams() {
        Assertions.assertEquals(1, Optional.ofNullable("1").map(Util::parseIntOrNull).orElse(-1));
        Assertions.assertEquals(-1, Optional.ofNullable((String)null).map(Util::parseIntOrNull).orElse(-1));
        Assertions.assertEquals(-1, Optional.ofNullable("").map(Util::parseIntOrNull).orElse(-1));
        Assertions.assertEquals(-1, Optional.ofNullable("bang").map(Util::parseIntOrNull).orElse(-1));
    }
}