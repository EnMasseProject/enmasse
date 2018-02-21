/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.*;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AddressTest {
    @Test
    public void testCreateFromBuilder() {
        Address.Builder b1 = new Address.Builder()
                .setAddress("addr1")
                .setAddressSpace("space1")
                .setName("myname")
                .setType("queue")
                .setPlan("myplan")
                .setStatus(new Status(true))
                .setUuid("myuuid");

        Address a1 = b1.build();

        Address.Builder b2 = new Address.Builder(a1);

        Address a2 = b2.build();

        assertThat(a1.getAddress(), is(a2.getAddress()));
        assertThat(a1.getAddressSpace(), is(a2.getAddressSpace()));
        assertThat(a1.getName(), is(a2.getName()));
        assertThat(a1.getPlan(), is(a2.getPlan()));
        assertThat(a1.getStatus(), is(a2.getStatus()));
        assertThat(a1.getType(), is(a2.getType()));
        assertThat(a1.getUuid(), is(a2.getUuid()));
    }

    @Test
    public void testSanitizer() {
        Address b1 = new Address.Builder()
                .setAddress("myAddr_-")
                .setPlan("p1")
                .setType("t1")
                .build();

        Address b2 = new Address.Builder()
                .setAddress(b1.getAddress())
                .setName(b1.getName())
                .setPlan(b1.getPlan())
                .setType(b1.getType())
                .build();
        System.out.println(b1.getName());
        assertTrue(b1.getName().startsWith("myaddr1-"));
        assertThat(b1.getName(), is(b2.getName()));
        assertThat(b1.getAddress(), is(b2.getAddress()));
        assertThat(b1.getPlan(), is(b2.getPlan()));
        assertThat(b1.getType(), is(b2.getType()));
    }
}
