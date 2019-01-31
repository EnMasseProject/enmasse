/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;

public class HttpAddressServiceBaseTest {

    @Test
    public void testOverride () {
        final Address address1 = new AddressBuilder()
                .withNewMetadata()
                .addToAnnotations("foo1", "bar1")
                .addToAnnotations("foo2", "bar2")
                .addToAnnotations("foo3", "bar3")
                .addToAnnotations("foo5", "bar5")
                .endMetadata()
                .build();

        final Address address2 = new AddressBuilder(address1)
                .editOrNewMetadata()
                .removeFromAnnotations("foo2")
                .removeFromAnnotations("foo5")
                .addToAnnotations("foo3", "bar3a")
                .addToAnnotations("foo4", "bar4")
                .addToAnnotations("foo6", "bar6")
                .endMetadata()
                .build();

        HttpAddressServiceBase.overrideAnnotation(address1, address2, "foo1");
        HttpAddressServiceBase.overrideAnnotation(address1, address2, "foo2");
        HttpAddressServiceBase.overrideAnnotation(address1, address2, "foo3");
        HttpAddressServiceBase.overrideAnnotation(address1, address2, "foo4");
        HttpAddressServiceBase.overrideAnnotation(address1, address2, "foo6");
        HttpAddressServiceBase.overrideAnnotation(address1, address2, "foo7");

        assertThat(address2.getMetadata().getAnnotations().get("foo1"), is("bar1"));
        assertThat(address2.getMetadata().getAnnotations().get("foo2"), is("bar2"));
        assertThat(address2.getMetadata().getAnnotations().get("foo3"), is("bar3"));
        assertThat(address2.getMetadata().getAnnotations().get("foo4"), is("bar4"));
        assertThat(address2.getMetadata().getAnnotations().get("foo5"), nullValue());
        assertThat(address2.getMetadata().getAnnotations().get("foo6"), is("bar6"));
        assertThat(address2.getMetadata().getAnnotations().get("foo7"), nullValue());
    }

    @Test
    public void testWithNull1 () {
        final Address address1 = new AddressBuilder()
                .withNewMetadata()
                .endMetadata()
                .build();

        final Address address2 = new AddressBuilder(address1)
                .editOrNewMetadata()
                .addToAnnotations("foo1", "bar1")
                .endMetadata()
                .build();

        address1.getMetadata().setAnnotations(null);

        HttpAddressServiceBase.overrideAnnotation(address1, address2, "foo1");

        assertThat(address2.getMetadata().getAnnotations().get("foo1"), is("bar1"));
    }

    @Test
    public void testWithNull2 () {
        final Address address1 = new AddressBuilder()
                .withNewMetadata()
                .addToAnnotations("foo1", "bar1")
                .endMetadata()
                .build();

        final Address address2 = new AddressBuilder(address1)
                .editOrNewMetadata()
                .endMetadata()
                .build();

        address2.getMetadata().setAnnotations(null);

        HttpAddressServiceBase.overrideAnnotation(address1, address2, "foo1");

        assertThat(address2.getMetadata().getAnnotations().get("foo1"), is("bar1"));
    }

}
