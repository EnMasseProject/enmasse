/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard;

import org.junit.jupiter.api.Test;

import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedStandard;

public class DummySharedTest extends TestBase implements ITestSharedStandard {

    @Test
    void test() {
        log.warn(resourcesManager.getSharedAddressSpace().toString());
    }

    @Test
    void test2() {
        log.warn("test2");
    }


    @Test
    void test3() {
        log.warn("test3");
    }

}
