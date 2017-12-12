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
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Status;
import io.enmasse.address.model.types.brokered.BrokeredAddressSpaceType;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.NoneAuthenticationServiceResolver;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(VertxUnitRunner.class)
public class ControllerTest {
    private Vertx vertx;
    private TestAddressSpaceApi testApi;
    private Kubernetes kubernetes;
    private OpenShiftClient client;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
        client = mock(OpenShiftClient.class);
        kubernetes = mock(Kubernetes.class);
        testApi = new TestAddressSpaceApi();

        when(kubernetes.withNamespace(anyString())).thenReturn(kubernetes);
        when(kubernetes.getNamespace()).thenReturn("myspace");
        when(kubernetes.existsNamespace(anyString())).thenReturn(true);
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testController(TestContext context) throws Exception {
        EventLogger testLogger = mock(EventLogger.class);
        Controller controller = new Controller(client, testApi, kubernetes, (a) -> new NoneAuthenticationServiceResolver("localhost", 1234), testLogger, null);

        vertx.deployVerticle(controller, context.asyncAssertSuccess());

        AddressSpace a1 = new AddressSpace.Builder()
                .setName("myspace")
                .setType(new StandardAddressSpaceType())
                .setStatus(new Status(false))
                .build();

        AddressSpace a2 = new AddressSpace.Builder()
                .setName("myspace")
                .setType(new BrokeredAddressSpaceType())
                .setStatus(new Status(false))
                .build();

        controller.resourcesUpdated(Sets.newSet(a1, a2));

        for (AddressSpace space : testApi.listAddressSpaces()) {
            assertTrue(space.getStatus().isReady());
        }
    }

}

