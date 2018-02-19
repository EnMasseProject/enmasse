/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.api.v1;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.TestSchemaApi;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.SecurityContext;

public class AddressApiHelperTest {

    private AddressApiHelper helper;
    private AddressApi addressApi;
    private SecurityContext securityContext;

    @Before
    public void setup() {
        AddressSpace addressSpace = mock(AddressSpace.class);
        when(addressSpace.getType()).thenReturn("type1");
        AddressSpaceApi addressSpaceApi = mock(AddressSpaceApi.class);
        addressApi = mock(AddressApi.class);
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(new BasicUserPrincipal("me"));
        when(securityContext.isUserInRole(any())).thenReturn(true);
        when(addressSpaceApi.getAddressSpaceWithName(eq("test"))).thenReturn(Optional.of(addressSpace));
        when(addressSpaceApi.withAddressSpace(eq(addressSpace))).thenReturn(addressApi);
        helper = new AddressApiHelper(addressSpaceApi, new TestSchemaApi());
    }

    @Test
    public void testPutAddresses() throws Exception {
        final Set<Address> addresses = new HashSet<>();
        when(addressApi.listAddresses()).thenReturn(Collections.singleton(createAddress("q1")));
        addresses.add(createAddress("q1"));
        addresses.add(createAddress("q2"));
        helper.putAddresses(securityContext,"test", new AddressList(addresses));
        verify(addressApi, never()).deleteAddress(any());
        verify(addressApi).createAddress(eq(createAddress("q2")));
    }

    private Address createAddress(final String name)
    {
        return new Address.Builder().setAddress(name).setAddressSpace("test").setType("queue").setPlan("plan1").build();
    }
}
