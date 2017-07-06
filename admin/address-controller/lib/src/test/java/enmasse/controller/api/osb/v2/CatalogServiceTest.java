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

import enmasse.controller.api.TestAddressSpaceApi;
import enmasse.controller.api.osb.v2.catalog.CatalogResponse;
import enmasse.controller.api.osb.v2.catalog.OSBCatalogService;
import enmasse.controller.api.osb.v2.catalog.Plan;
import enmasse.controller.api.osb.v2.catalog.Service;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.Response;

public class CatalogServiceTest {

    @Test
    public void testCatalog() throws IOException {
        OSBCatalogService catalogService = new OSBCatalogService(new TestAddressSpaceApi());

        Response response = catalogService.getCatalog();
        CatalogResponse catalogResponse = (CatalogResponse) response.getEntity();
        List<Service> services = catalogResponse.getServices();

        assertThat(services.size(), is(4));
        assertService(services.get(0), "enmasse-anycast", "standard");
        assertService(services.get(1), "enmasse-multicast", "standard");
        assertService(services.get(2), "enmasse-queue", "inmemory", "persisted", "pooled-inmemory", "pooled-persisted");
        assertService(services.get(3), "enmasse-topic", "inmemory", "persisted");
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
}
