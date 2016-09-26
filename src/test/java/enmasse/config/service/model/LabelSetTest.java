/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.config.service.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LabelSetTest {
    @Test
    public void testContains() {
        LabelSet fullSet = LabelSet.fromString("key1=value1,key2=value2,key3=value3");
        assertTrue(fullSet.contains(LabelSet.fromString("key1=value1")));
        assertTrue(fullSet.contains(LabelSet.fromString("key2=value2")));
        assertTrue(fullSet.contains(LabelSet.fromString("key3=value3")));

        assertTrue(fullSet.contains(LabelSet.fromString("key1=value1,key2=value2")));
        assertTrue(fullSet.contains(LabelSet.fromString("key2=value2,key3=value3")));
        assertTrue(fullSet.contains(LabelSet.fromString("key1=value1,key3=value3")));

        assertFalse(fullSet.contains(LabelSet.fromString("key4=value4")));
        assertFalse(fullSet.contains(LabelSet.fromString("key1=value1,key2=value2,key3=value3,key4=value4")));
        assertFalse(fullSet.contains(LabelSet.fromString("key1=value2")));
        assertFalse(fullSet.contains(LabelSet.fromString("key2=value1")));
    }
}
