/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.bases.JUnitWorkaround;

@Tag(TestTag.FRAMEWORK)
@ExtendWith(JUnitWorkaround.class)
@Disabled("This is just for local testing during development")
public class JUnitWorkaroundTest {

    @BeforeAll
    public static void beforeAllFailure() {
        JUnitWorkaround.wrapBeforeAll(() -> {
            throw new RuntimeException("Expected failure");
        });
        System.out.println("Done");
    }

    @Test
    public void testExpectedFailure1() {
        System.out.println("You should never see this");
    }

    @Test
    public void testExpectedFailure2() {
        System.out.println("You should never see this");
    }

}
