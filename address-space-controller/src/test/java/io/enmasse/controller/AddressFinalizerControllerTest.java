/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.model.CustomResourceDefinitions;

public class AddressFinalizerControllerTest {

    private AddressFinalizerController controller;
    private AddressSpaceApi addressSpaceApi;

    @BeforeAll
    public static void init () {
        CustomResourceDefinitions.registerAll();
    }

    @BeforeEach
    public void setup() {
        this.addressSpaceApi = new TestAddressSpaceApi();
        this.controller = new AddressFinalizerController(addressSpaceApi);
    }

    /**
     * Test calling the finalizer controller with a non-deleted object.
     */
    @Test
    public void testAddingFinalizer () throws Exception {

        // given a non-delete object

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("foo")
                .withNamespace("bar")
                .withFinalizers("foo/bar")
                .endMetadata()
                .build();

        // when calling the reconcile method of the finalizer controller

        Controller.ReconcileResult result = controller.reconcileAnyState(addressSpace);
        assertTrue(result.isPersistAndRequeue());
        addressSpace = result.getAddressSpace();

        // it should add the finalizer, but not remove existing finalizers

        assertThat(addressSpace, notNullValue());
        assertThat(addressSpace.getMetadata(), notNullValue());
        assertThat(addressSpace.getMetadata().getFinalizers(), hasItems("foo/bar", AddressFinalizerController.FINALIZER_ADDRESSES));

    }

    @Test
    public void testProcessingFinalizer () throws Exception {

        // given a deleted address space, with the finalizer still present

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("foo")
                .withNamespace("bar")
                .withDeletionTimestamp(Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .withFinalizers("foo/bar", AddressFinalizerController.FINALIZER_ADDRESSES)
                .endMetadata()
                .build();

        addressSpaceApi.createAddressSpace(addressSpace);
        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);

        // and given an existing address for this address space

        Address address = new AddressBuilder()
                .withNewMetadata()
                .withName("foo.foo")
                .withNamespace("bar")
                .endMetadata()
                .build();

        addressApi.createAddress(address);

        // and given another address space and address

        AddressSpace otherAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("foo2")
                .withNamespace("bar")
                .withFinalizers("foo/bar", AddressFinalizerController.FINALIZER_ADDRESSES)
                .endMetadata()
                .build();

        addressSpaceApi.createAddressSpace(otherAddressSpace);
        AddressApi otherAddressApi = addressSpaceApi.withAddressSpace(otherAddressSpace);

        // and given an existing address for this address space

        Address otherAddress = new AddressBuilder()
                .withNewMetadata()
                .withName("foo2.foo")
                .withNamespace("bar")
                .endMetadata()
                .build();

        otherAddressApi.createAddress(otherAddress);

        // ensure that the address spaces and addresses should be found

        assertThat(addressSpaceApi.getAddressSpaceWithName("bar", "foo").orElse(null), notNullValue());
        assertThat(addressApi.getAddressWithName("bar", "foo.foo").orElse(null), notNullValue());
        assertThat(addressSpaceApi.getAddressSpaceWithName("bar", "foo2").orElse(null), notNullValue());
        assertThat(otherAddressApi.getAddressWithName("bar", "foo2.foo").orElse(null), notNullValue());

        // when running the reconcile method

        Controller.ReconcileResult result = controller.reconcileAnyState(addressSpace);
        assertTrue(result.isPersistAndRequeue());
        addressSpace = result.getAddressSpace();

        // then the finalizer of this controller should be removed, and the address should be deleted

        assertThat(addressSpace, notNullValue());
        assertThat(addressSpace.getMetadata(), notNullValue());
        assertThat(addressSpace.getMetadata().getFinalizers(), hasItems("foo/bar"));

        assertThat(addressSpaceApi.getAddressSpaceWithName("bar", "foo").orElse(null), notNullValue());
        assertThat(addressApi.getAddressWithName("bar", "foo.foo").orElse(null), nullValue());

        // but not the "other" address

        assertThat(addressSpaceApi.getAddressSpaceWithName("bar", "foo2").orElse(null), notNullValue());
        assertThat(otherAddressApi.getAddressWithName("bar", "foo2.foo").orElse(null), notNullValue());
    }
}
