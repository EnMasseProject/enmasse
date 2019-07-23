/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtests.utils;



import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

import io.enmasse.systemtest.utils.TestUtils;

public class TestUtilsTest {

    @Test
    public void testLength0() {
        assertThat(TestUtils.randomCharacters(0).length(), is(0));
    }

    @Test
    public void testLength10() {
        assertThat(TestUtils.randomCharacters(10).length(), is(10));
    }

    @Test
    public void testLength11() {
        assertThat(TestUtils.randomCharacters(11).length(), is(11));
    }

}
