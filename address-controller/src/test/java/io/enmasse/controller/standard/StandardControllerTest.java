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
package io.enmasse.controller.standard;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.NoneAuthenticationServiceResolver;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
public class StandardControllerTest {
    private Vertx vertx;
    private TestAddressSpaceApi testApi;
    private Kubernetes kubernetes;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
        kubernetes = mock(Kubernetes.class);
        EventLogger mockLogger = mock(EventLogger.class);
        when(kubernetes.withNamespace(any())).thenReturn(kubernetes);
        when(kubernetes.createEventLogger(any(), any())).thenReturn(mockLogger);
        testApi = new TestAddressSpaceApi();
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testStandardController(TestContext context) throws Exception {
        StandardController controller = new StandardController(vertx, testApi, kubernetes, authenticationServiceType -> new NoneAuthenticationServiceResolver("localhost", 1234), null);
        AddressSpace a1 = new AddressSpace.Builder()
                .setName("myspace")
                .setType(new StandardAddressSpaceType())
                .build();

        assertThat(controller.getAddressSpaceType().getName(), is(new StandardAddressSpaceType().getName()));
        controller.resourcesUpdated(Sets.newSet(a1));

        assertThat(testApi.listAddressSpaces().size(), is(1));
        assertThat(testApi.listAddressSpaces(), hasItem(a1));
    }
}
