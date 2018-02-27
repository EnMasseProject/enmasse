/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("mytest")
public class Junit5Test {
    @Test
    public void myTest() {
        System.out.println("FOO");
    }
}
