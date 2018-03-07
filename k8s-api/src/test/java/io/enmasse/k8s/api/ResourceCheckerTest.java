/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.time.Duration;
import java.util.Set;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ResourceCheckerTest {
    ResourceChecker<String> controller;
    Watcher<String> watcher;

    @Before
    public void setup() {
        watcher = mock(Watcher.class);
        controller = new ResourceChecker<>(watcher, Duration.ofMillis(1));
    }

    @Test
    public void testResourcesUpdated() throws Exception {
        Set<String> items = Sets.newSet("hello", "there");
        controller.onUpdate(items);
        controller.doWork();
        verify(watcher).onUpdate(eq(items));
    }
}
