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

package enmasse.controller.flavor;

import enmasse.controller.model.Flavor;
import enmasse.controller.flavor.FlavorManager;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FlavorManagerTest {
    private FlavorManager flavorManager;

    @Before
    public void setup() {
        flavorManager = new FlavorManager();
    }

    @Test
    public void testManager() {
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put("flavor1", new Flavor.Builder("flavor1", "template1").build());
        flavorMap.put("flavor2", new Flavor.Builder("flavor2", "template2").build());
        flavorMap.put("flavor3", new Flavor.Builder("flavor3", "template1").templateParameter("BROKER_IMAGE", "myimage").build());

        flavorManager.flavorsUpdated(flavorMap);

        assertThat(flavorManager.getFlavor("flavor1", 0).templateName(), is("template1"));
        assertThat(flavorManager.getFlavor("flavor1", 0).templateParameters().size(), is(0));
        assertThat(flavorManager.getFlavor("flavor2", 0).templateName(), is("template2"));
        assertThat(flavorManager.getFlavor("flavor2", 0).templateParameters().size(), is(0));
        assertThat(flavorManager.getFlavor("flavor3", 0).templateName(), is("template1"));
        assertThat(flavorManager.getFlavor("flavor3", 0).templateParameters().size(), is(1));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testTimeout() {
        flavorManager.getFlavor("flavor1", 1000);
    }
}
