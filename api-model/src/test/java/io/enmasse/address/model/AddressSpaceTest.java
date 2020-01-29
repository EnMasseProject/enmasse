/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import javax.validation.ValidationException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AddressSpaceTest {

    @Test
    public void testSimpleCreateFromBuilder() {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("name")
                .endMetadata()

                .withNewSpec()
                .withType("type")
                .withPlan("plan")
                .endSpec()

                .build();

        assertNotNull(space);

        assertThat(space.getMetadata().getName(), is("name"));
        assertThat(space.getSpec().getType(), is("type"));
        assertThat(space.getSpec().getPlan(), is("plan"));
        assertThat(space.getStatus(), is(new AddressSpaceStatus(false)));
        assertNotNull(space.getSpec().getEndpoints());
        assertThat(space.getSpec().getEndpoints().size(), is(0));
        assertNull(space.getSpec().getAuthenticationService());

        assertNotNull(space.getMetadata().getAnnotations());
        assertThat(space.getMetadata().getAnnotations().size(), is(0));
        assertNotNull(space.getMetadata().getLabels());
        assertThat(space.getMetadata().getLabels().size(), is(0));
    }


    @Test
    @Disabled
    public void testSimpleWithMissingMandatory() {
        try {
            new AddressSpaceBuilder()
                    .withNewSpec()
                    .withType("type")
                    .withPlan("plan")
                    .endSpec()
                    .build();
            fail("");
        } catch (ValidationException e) {
            // pass
        }

        try {
            new AddressSpaceBuilder()
                    .withNewMetadata()
                    .withName("name")
                    .endMetadata()

                    .withNewSpec()
                    .withPlan("plan")
                    .endSpec()
                    .build();
            fail("");
        } catch (ValidationException e) {
            // pass
        }

        try {
            new AddressSpaceBuilder()
                    .withNewMetadata()
                    .withName("name")
                    .endMetadata()

                    .withNewSpec()
                    .withType("type")
                    .endSpec()
                    .build();
            fail("");
        } catch (ValidationException e) {
            // pass
        }
    }

    @Test
    public void testEqualityIsBasedOnNameAndNamespace() {
        AddressSpace space1 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("name")
                .endMetadata()

                .withNewSpec()
                .withType("type")
                .withPlan("plan")
                .endSpec()
                .build();
        AddressSpace space2 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("name")
                .endMetadata()

                .withNewSpec()
                .withType("type2")
                .withPlan("plan")
                .endSpec()
                .build();
        assertEquals(space1, space2);

        AddressSpace space3 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("name")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withType("type2")
                .withPlan("plan")
                .endSpec()
                .build();
        assertNotEquals(space1, space3);
        AddressSpace space4 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("name")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withType("type")
                .withPlan("plan2")
                .endSpec()
                .build();
        assertEquals(space3, space4);
        AddressSpace space5 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("name2")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withType("type")
                .withPlan("plan2")
                .endSpec()
                .build();
        assertNotEquals(space4, space5);

    }
}
