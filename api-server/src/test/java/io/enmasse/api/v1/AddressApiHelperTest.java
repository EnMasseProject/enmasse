/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.v1;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import javax.ws.rs.BadRequestException;
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
        when(addressSpaceApi.getAddressSpaceWithName(any(), eq("test"))).thenReturn(Optional.of(addressSpace));
        when(addressSpaceApi.withAddressSpace(eq(addressSpace))).thenReturn(addressApi);
        helper = new AddressApiHelper(addressSpaceApi, new TestSchemaProvider());
    }

    @Test
    public void testPutAddresses() throws Exception {
        when(addressApi.listAddresses(any())).thenReturn(Collections.singleton(createAddress("q1")));
        helper.replaceAddress("test", createAddress("q1"));
        verify(addressApi, never()).deleteAddress(any());
        verify(addressApi).replaceAddress(eq(createAddress("q1")));
    }

    @Test
    public void testDuplicateAddresses() throws Exception {
        when(addressApi.listAddresses(any())).thenReturn(Sets.newSet(createAddress("q1"), createAddress("q2")));

        try {
            helper.createAddress("test", createAddress("q3", "q1"));
            fail("Expected exception for duplicate address");
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), is("Address 'q1' already exists with resource name 'q1'"));
        }
    }

    @Test
    public void testParseLabelSelector() throws Exception {
        Map<String, String> labels = AddressApiHelper.parseLabelSelector("key=value");
        assertThat(labels.size(), is(1));
        assertThat(labels.get("key"), is("value"));

        labels = AddressApiHelper.parseLabelSelector("key1=value1,key2=value2,key3=value3");
        assertThat(labels.size(), is(3));
        assertThat(labels.get("key1"), is("value1"));
        assertThat(labels.get("key2"), is("value2"));
        assertThat(labels.get("key3"), is("value3"));
    }

    private Address createAddress(String name, String address)
    {
        return new Address.Builder().setName(name).setNamespace("ns").setAddress(address).setAddressSpace("test").setType("queue").setPlan("plan1").build();
    }

    private Address createAddress(String address)
    {
        return createAddress(address, address);
    }
}
