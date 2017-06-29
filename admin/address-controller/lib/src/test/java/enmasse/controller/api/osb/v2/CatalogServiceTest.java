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

package enmasse.controller.api.osb.v2;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import enmasse.controller.api.TestInstanceApi;
import enmasse.controller.api.osb.v2.catalog.CatalogResponse;
import enmasse.controller.api.osb.v2.catalog.OSBCatalogService;
import enmasse.controller.api.osb.v2.catalog.Plan;
import enmasse.controller.api.osb.v2.catalog.Service;
import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.model.Flavor;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;

public class CatalogServiceTest {

    @Test
    public void testCatalog() throws IOException {
        FlavorManager flavorManager = new FlavorManager();
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put("flavor1", new Flavor.Builder("flavor1", "template1").type("queue").description("Simple queue").uuid(UUID.randomUUID().toString()).build());
        flavorMap.put("flavor2", new Flavor.Builder("flavor2", "template2").type("topic").description("Simple topic").uuid(UUID.randomUUID().toString()).build());
        flavorMap.put("flavor3", new Flavor.Builder("flavor3", "template3").type("topic").description("Another topic").uuid(UUID.randomUUID().toString()).build());
        flavorManager.flavorsUpdated(flavorMap);

        OSBCatalogService catalogService = new OSBCatalogService(new TestInstanceApi(), flavorManager);

        Response response = catalogService.getCatalog();
        CatalogResponse catalogResponse = (CatalogResponse) response.getEntity();
        List<Service> services = catalogResponse.getServices();

        assertThat(services.size(), is(4));
        assertService(services.get(0), "enmasse-anycast", "default");
        assertService(services.get(1), "enmasse-multicast", "default");
        assertService(services.get(2), "enmasse-queue", "flavor1");
        assertService(services.get(3), "enmasse-topic", "flavor2", "flavor3");
    }

    private void assertService(Service service, String name, String... planNames) {
        assertThat(service.getName(), is(name));
        assertThat(service.getPlans().size(), is(planNames.length));
        for (int i = 0; i < planNames.length; i++) {
            String planName = planNames[i];
            Plan plan = service.getPlans().get(i);
            assertThat(plan.getName(), is(planName));
        }
    }

    @Test
    public void testServiceWithoutFlavorsIsOmitted() throws IOException {
        FlavorManager flavorManager = new FlavorManager();
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        // NOTE: no flavors for queues
        flavorMap.put("flavor2", new Flavor.Builder("flavor2", "template2").type("topic").description("Simple topic").uuid(UUID.randomUUID().toString()).build());
        flavorManager.flavorsUpdated(flavorMap);

        OSBCatalogService catalogService = new OSBCatalogService(new TestInstanceApi(), flavorManager);

        Response response = catalogService.getCatalog();
        CatalogResponse catalogResponse = (CatalogResponse) response.getEntity();
        List<Service> services = catalogResponse.getServices();

        // TODO: is there a hamcrest matcher that does the same (and doesn't use reflection)?
        services.stream().forEach(s -> assertThat(s.getName(), not(is("enmasse-queue"))));
    }

}
