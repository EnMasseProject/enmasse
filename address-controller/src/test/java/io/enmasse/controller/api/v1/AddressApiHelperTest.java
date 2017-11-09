/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import io.enmasse.address.model.types.standard.StandardType;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
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
        AddressSpaceApi addressSpaceApi = mock(AddressSpaceApi.class);
        addressApi = mock(AddressApi.class);
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(new BasicUserPrincipal("me"));
        when(securityContext.isUserInRole(any())).thenReturn(true);
        when(addressSpaceApi.getAddressSpaceWithName(eq("test"))).thenReturn(Optional.of(addressSpace));
        when(addressSpaceApi.withAddressSpace(eq(addressSpace))).thenReturn(addressApi);
        helper = new AddressApiHelper(addressSpaceApi);
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
        return new Address.Builder().setName(name).setAddressSpace("test").setType(StandardType.QUEUE).setPlan(StandardType.QUEUE.getDefaultPlan()).build();
    }
}
