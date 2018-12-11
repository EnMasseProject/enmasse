/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.common.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.enmasse.common.api.model.CustomResource.Plural;
import io.enmasse.common.api.model.CustomResource.Singular;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;

public class CustomResourceTest {

    @CustomResource(version="v1alpha1", group = "iot.enmasse.io")
    private static class Foo {
    }

    @CustomResource(version="v1alpha1",group = "iot.enmasse.io", shortNames = { "b", "ba" })
    @Singular("baa")
    private static class Bar {
    }

    @CustomResource(version="v1alpha1",group = "iot.enmasse.io")
    @Singular
    @Plural("bazzes")
    private static class Baz {
    }

    @Test
    public void testResourceDefinition1() {
        final CustomResourceDefinition definition = CustomResources.createFromClass(Foo.class);

        assertEquals("foos.iot.enmasse.io", definition.getMetadata().getName());

        assertEquals("v1alpha1", definition.getSpec().getVersion());
        assertEquals("iot.enmasse.io", definition.getSpec().getGroup());

        assertEquals("Foo", definition.getSpec().getNames().getKind());
        assertEquals("foo", definition.getSpec().getNames().getSingular());
        assertEquals("foos", definition.getSpec().getNames().getPlural());
        assertEquals(Arrays.asList(), definition.getSpec().getNames().getShortNames());
    }

    @Test
    public void testResourceDefinition2() {
        final CustomResourceDefinition definition = CustomResources.createFromClass(Bar.class);

        assertEquals("baas.iot.enmasse.io", definition.getMetadata().getName());

        assertEquals("v1alpha1", definition.getSpec().getVersion());
        assertEquals("iot.enmasse.io", definition.getSpec().getGroup());

        assertEquals("Bar", definition.getSpec().getNames().getKind());
        assertEquals("baa", definition.getSpec().getNames().getSingular());
        assertEquals("baas", definition.getSpec().getNames().getPlural());
        assertEquals(Arrays.asList("b", "ba"), definition.getSpec().getNames().getShortNames());
    }

    @Test
    public void testResourceDefinition3() {
        final CustomResourceDefinition definition = CustomResources.createFromClass(Baz.class);

        assertEquals("bazzes.iot.enmasse.io", definition.getMetadata().getName());

        assertEquals("v1alpha1", definition.getSpec().getVersion());
        assertEquals("iot.enmasse.io", definition.getSpec().getGroup());

        assertEquals("Baz", definition.getSpec().getNames().getKind());
        assertNull(definition.getSpec().getNames().getSingular());
        assertEquals("bazzes", definition.getSpec().getNames().getPlural());
        assertEquals(Arrays.asList(), definition.getSpec().getNames().getShortNames());
    }

}
