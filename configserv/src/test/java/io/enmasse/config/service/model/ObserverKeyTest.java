/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.model;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ObserverKeyTest {
    @Test
    public void testHashCode() {
        ObserverKey key1 = new ObserverKey(Collections.singletonMap("role", "broker"),  Collections.emptyMap());
        ObserverKey key2 = new ObserverKey(Collections.singletonMap("role", "broker"),  Collections.singletonMap("cluster_id", "mytopic"));

        Set<ObserverKey> keys = new HashSet<>();
        keys.add(key1);
        keys.add(key2);

        assertThat(keys.size(), is(2));
        assertThat(keys, hasItem(key1));
        assertThat(keys, hasItem(key2));
    }
}
