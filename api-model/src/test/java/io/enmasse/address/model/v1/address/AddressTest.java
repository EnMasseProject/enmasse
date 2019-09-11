/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.Phase;
import io.enmasse.address.model.AddressStatus;
import org.junit.jupiter.api.Test;

import static io.enmasse.address.model.validation.ValidationMatchers.isValid;
import static io.enmasse.address.model.validation.ValidationMatchers.isNotValid;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressTest {
    @Test
    public void testCreateFromBuilder() {
        AddressBuilder b1 = new AddressBuilder()

                .withNewMetadata()
                .withName("myspace.myname")
                .withUid("myuid")
                .withResourceVersion("1234")
                .withSelfLink("/my/link")
                .withCreationTimestamp("my stamp")
                .endMetadata()

                .withNewSpec()
                .withAddress("addr1")
                .withAddressSpace("space1")
                .withType("queue")
                .withPlan("myplan")
                .endSpec()

                .withNewStatus(true);

        Address a1 = b1.build();

        AddressBuilder b2 = new AddressBuilder(a1);

        Address a2 = b2.build();

        assertThat(a1.getSpec().getAddress(), is(a2.getSpec().getAddress()));
        assertThat(Address.extractAddressSpace(a1), is(Address.extractAddressSpace(a2)));
        assertThat(a1.getSpec().getPlan(), is(a2.getSpec().getPlan()));
        assertThat(a1.getStatus(), is(a2.getStatus()));
        assertThat(a1.getSpec().getType(), is(a2.getSpec().getType()));

        assertThat(a1.getMetadata().getName(), is(a2.getMetadata().getName()));
        assertThat(a1.getMetadata().getUid(), is(a2.getMetadata().getUid()));
        assertThat(a1.getMetadata().getResourceVersion(), is(a2.getMetadata().getResourceVersion()));
        assertThat(a1.getMetadata().getSelfLink(), is(a2.getMetadata().getSelfLink()));
        assertThat(a1.getMetadata().getCreationTimestamp(), is(a2.getMetadata().getCreationTimestamp()));
    }

    @Test
    public void testSanitizer() {
        Address b1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace("ns1")
                .endMetadata()

                .withNewSpec()
                .withAddress("myAddr_-")
                .withAddressSpace("myspace")
                .withPlan("p1")
                .withType("t1")
                .endSpec()

                .build();

        Address b2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace("ns1")
                .withName(b1.getMetadata().getName())
                .endMetadata()

                .withNewSpec()
                .withAddress(b1.getSpec().getAddress())
                .withAddressSpace("myspace")
                .withPlan(b1.getSpec().getPlan())
                .withType(b1.getSpec().getType())
                .endSpec()

                .build();

        assertNull(b1.getMetadata().getName());
        @SuppressWarnings("deprecation")
        String generated = Address.generateName(b1.getSpec().getAddressSpace(), b1.getSpec().getAddress());
        System.out.println(generated);
        assertTrue(generated.startsWith("myspace.myaddr1."));
        assertThat(b1.getMetadata().getName(), is(b2.getMetadata().getName()));
        assertThat(b1.getSpec().getAddress(), is(b2.getSpec().getAddress()));
        assertThat(b1.getSpec().getPlan(), is(b2.getSpec().getPlan()));
        assertThat(b1.getSpec().getType(), is(b2.getSpec().getType()));
    }

    @Test
    public void testCopy() {
        Address a = new AddressBuilder()
                .withNewMetadata()
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("a1")
                .withPlan("p1")
                .withType("t1")
                .withAddressSpace("myspace")
                .endSpec()

                .withStatus(new AddressStatus(true).setPhase(Phase.Active).appendMessage("foo"))
                .build();

        Address b = new AddressBuilder(a).build();

        assertThat(a, is(b));
        assertTrue(b.getStatus().isReady());
        assertThat(b.getStatus().getPhase(), is(Phase.Active));
        assertThat(b.getStatus().getMessages(), hasItem("foo"));
    }

    /**
     * Ensure that both objects are independent after they got copied
     */
    @Test
    public void testCopyIndependence() {
        Address a = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("a1")
                .withPlan("p1")
                .withType("t1")
                .endSpec()

                .withStatus(new AddressStatus(true).setPhase(Phase.Active).appendMessage("foo"))
                .build();

        Address b = new AddressBuilder(a).build();

        assertThat(a.getMetadata().getName(), is("myspace.a"));
        assertThat(b.getMetadata().getName(), is("myspace.a"));

        b.getMetadata().setName("myspace.b");

        assertThat(a.getMetadata().getName(), is("myspace.a"));
        assertThat(b.getMetadata().getName(), is("myspace.b"));

    }

    /**
     * Create a simple valid address.
     * @param name the name for {@code .metadata.name}
     * @return the newly created address, this address may not be valid
     */
    private static Address createAddress(final String name) {
        return new AddressBuilder(false)
                .withNewMetadata()
                .withName(name)
                .endMetadata()

                .withNewSpec()
                .withAddress(name)
                .withPlan("plan")
                .withType("type")
                .endSpec()

                .build();
    }

    /**
     * Test if a basic address name validates.
     */
    @Test
    public void testAddressNameValidation1() {
        assertThat(createAddress("foo.bar"), isValid());
    }

    /**
     * Test if a basic address name validates.
     */
    @Test
    public void testAddressNameValidation2() {
        assertThat(createAddress("bar"), isNotValid());
    }

    private static final String FOURTY = "01234567890123456789012345678901234567890123456789";
    private static final String EIGHTY = FOURTY + FOURTY;

    /**
     * Test if a long name validates.
     * <br>
     * The combined must validate as a kubernetes name, although it is longer than 60 characters.
     */
    @Test
    public void testLongAddressNameValidation() {
        assertThat(createAddress(FOURTY + "." + FOURTY), isValid());
    }

    /**
     * Test if a too long name does not validate.
     * <br>
     * Name components which are longer than 60 character must not validate.
     */
    @Test
    public void testTooLongAddressNameValidation() {
        assertThat(createAddress(EIGHTY + ".foo"), isNotValid());
        assertThat(createAddress("foo." + EIGHTY), isNotValid());
    }

    /**
     * When the {@code .spec.addressSpace} field is set, then it must match.
     */
    @Test
    public void testAddressSpaceMismatch () {
        final Address a = createAddress("foo.addr");
        a.getSpec().setAddressSpace("bar");

        assertThat(a, isNotValid());
    }

    /**
     * When the {@code .spec.addressSpace} field is set, then it must match, even when it is in a list.
     */
    @Test
    public void testAddressSpaceMismatchInList () {
        final Address a = createAddress("foo.addr");
        a.getSpec().setAddressSpace("bar");

        final AddressList list = new AddressList();
        list.getItems().add(a);

        assertThat(list, isNotValid());
    }

    /**
     * When the metadata name is missing, the address space name must still validate.
     */
    @Test
    public void testAddressSpaceNoName () {
        final Address a = createAddress(null);
        a.getSpec().setAddress("foo");
        a.getSpec().setAddressSpace("bar");

        assertThat(a, isValid());
    }
}
