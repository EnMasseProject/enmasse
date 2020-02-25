/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.model.CustomResourceDefinitions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ComponentFinalizerControllerTest {

    private ComponentFinalizerController controller;
    private Kubernetes client;

    @BeforeAll
    public static void init() {
        CustomResourceDefinitions.registerAll();
    }

    @BeforeEach
    public void setup() {
        this.client = mock(Kubernetes.class);
        this.controller = new ComponentFinalizerController(client);
    }

    @Test
    public void testFinalizerSuccess() throws Exception {
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace());
        assertNotNull(result);
        assertTrue(result.isFinalized());
        verify(client).deleteResources(eq("1234"));
    }

    @Test
    public void testFinalizerFailure() throws Exception {
        doThrow(new RuntimeException("ERROR")).when(client).deleteResources(any());
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace());
        assertNotNull(result);
        assertFalse(result.isFinalized());
    }

    private static AddressSpace createTestSpace() {
        return new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withName("myspace")
                .withNamespace("test")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .withType("standard")
                .withPlan("standard")
                .endSpec()
                .build();
    }
}
