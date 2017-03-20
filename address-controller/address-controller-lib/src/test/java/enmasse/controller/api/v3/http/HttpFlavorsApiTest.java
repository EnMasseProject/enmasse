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

package enmasse.controller.api.v3.http;

import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.api.v3.FlavorList;
import enmasse.controller.model.Flavor;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HttpFlavorsApiTest {
    private FlavorManager flavorManager;
    private FlavorsService flavorsService;
    private Set<Flavor> expectedFlavors;

    @Before
    public void setup() {
        flavorManager = new FlavorManager();
        flavorsService = new FlavorsService(flavorManager);
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put("flavor1", new Flavor.Builder("flavor1", "template1").type("queue").description("Simple queue").build());
        flavorMap.put("flavor2", new Flavor.Builder("flavor2", "template2").type("topic").description("Simple topic").build());
        expectedFlavors = new LinkedHashSet<>(flavorMap.values());
        flavorManager.flavorsUpdated(flavorMap);
    }

    @Test
    public void testList() throws IOException {
        Response response = flavorsService.listFlavors();
        FlavorList list = (FlavorList) response.getEntity();

        assertThat(list.getFlavors(), is(expectedFlavors));
    }

    @Test
    public void testGet() throws IOException {
        Response response = flavorsService.getFlavor("flavor1");

        enmasse.controller.api.v3.Flavor flavor = (enmasse.controller.api.v3.Flavor) response.getEntity();
        Flavor f = flavor.getFlavor();
        assertThat(f.name(), is("flavor1"));
        assertThat(f.type(), is("queue"));
        assertThat(f.description(), is("Simple queue"));
    }

    @Test
    public void testGetUnknown() throws IOException {
        Response response = flavorsService.getFlavor("unknown");
        assertThat(response.getStatus(), is(404));
    }
}
