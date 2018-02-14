/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Schema;
import io.enmasse.address.model.Status;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.NoneAuthenticationServiceResolver;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.SchemaApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.api.TestSchemaApi;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;

import java.util.Collections;

import static org.junit.Assert.assertTrue;
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
        SchemaApi schemaApi = new TestSchemaApi();
        Controller controller = new Controller(client, testApi, kubernetes, (a) -> new NoneAuthenticationServiceResolver("localhost", 1234), testLogger, null, schemaApi);

        vertx.deployVerticle(controller, context.asyncAssertSuccess());

        AddressSpace a1 = new AddressSpace.Builder()
                .setName("myspace")
                .setType("type1")
                .setPlan("myplan")
                .setStatus(new Status(false))
                .build();

        AddressSpace a2 = new AddressSpace.Builder()
                .setName("myspace2")
                .setType("type1")
                .setPlan("myplan")
                .setStatus(new Status(false))
                .build();

        controller.resourcesUpdated(Sets.newSet(a1, a2));

        for (AddressSpace space : testApi.listAddressSpaces()) {
            assertTrue(space.getStatus().isReady());
        }
    }

}

