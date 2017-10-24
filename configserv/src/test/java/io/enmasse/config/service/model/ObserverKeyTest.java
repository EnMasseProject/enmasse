/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
