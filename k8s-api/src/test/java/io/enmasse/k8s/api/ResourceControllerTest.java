/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceControllerTest {
    TestResource resource;
    TestWatcher watcher;
    ResourceController<String> controller;

    @Before
    public void setup() {
        resource = new TestResource();
        watcher = new TestWatcher();
        controller = new ResourceController<>(resource, watcher, new Supplier<Long>() {
            @Override
            public Long get() {
                return 2_000L;
            }
        }, true);
        controller.start();
    }

    @After
    public void teardown() {
        controller.stop();
    }

    @Test
    public void testResourcesAdded() {
        resource.addResource("r1");
        assertResource("r1");

        resource.addResource("r2");
        assertResource("r1");
        assertResource("r2");
    }

    @Test
    public void testResourcesRemoved() {
        resource.addResource("r1").addResource("r2").addResource("r3");
        assertResource("r1");
        assertResource("r2");
        assertResource("r3");

        resource.removeResource("r2");
        assertResource("r1");
        assertNotResource("r2");
        assertResource("r3");
    }

    private void assertResource(String name) {
        long end = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < end && !watcher.getResources().contains(name)) { }
        assertTrue(watcher.getResources().contains(name));
    }

    private void assertNotResource(String name) {
        long end = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < end && watcher.getResources().contains(name)) { }
        assertFalse(watcher.getResources().contains(name));
    }

    private static class TestResource implements Resource<String> {

        private Set<String> resources = new HashSet<>();

        @Override
        public Watch watchResources(Watcher watcher) {
            return () -> {};
        }

        public synchronized TestResource addResource(String resource) {
            this.resources.add(resource);
            return this;
        }

        @Override
        public synchronized Set<String> listResources() {
            return new HashSet<>(resources);
        }

        public synchronized void removeResource(String resource) {
            this.resources.remove(resource);
        }
    }

    private static class TestWatcher implements io.enmasse.k8s.api.Watcher<String> {
        private Set<String> resources = new HashSet<>();

        public synchronized Set<String> getResources() {
            return new HashSet<>(resources);
        }

        @Override
        public synchronized void resourcesUpdated(Set<String> resources) throws Exception {
            this.resources = new HashSet<>(resources);
        }
    }
}
