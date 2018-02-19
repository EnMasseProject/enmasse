/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.v1.http;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.v1.Either;
import io.enmasse.controller.api.DefaultExceptionMapper;
import io.enmasse.k8s.api.TestAddressApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.api.TestSchemaApi;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpAddressServiceTest {
    private HttpAddressService addressService;
    private TestAddressSpaceApi addressSpaceApi;
    private TestAddressApi addressApi;
    private Address q1;
    private Address a1;
    private SecurityContext securityContext;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();

    @Before
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        this.addressService = new HttpAddressService(addressSpaceApi, new TestSchemaApi());

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setType("type1")
                .setPlan("myplan")
                .build();

        securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(new BasicUserPrincipal("me"));
        when(securityContext.isUserInRole(any())).thenReturn(true);

        addressSpaceApi.createAddressSpace(addressSpace);
        addressApi = (TestAddressApi) addressSpaceApi.withAddressSpace(addressSpace);
        q1 = new Address.Builder()
                .setAddress("q1")
                .setType("queue")
                .build();
        a1 = new Address.Builder()
                .setAddress("a1")
                .setType("anycast")
                .build();
        addressApi.createAddress(q1);
        addressApi.createAddress(a1);
    }

    private Response invoke(Callable<Response> fn) {
        try {
            return fn.call();
        } catch (Exception e) {
            return exceptionMapper.toResponse(e);
        }
    }

    @Test
    public void testList() {
        Response response = invoke(() -> addressService.getAddressList(securityContext,"myspace"));

        assertThat(response.getStatus(), is(200));
        AddressList list = (AddressList) response.getEntity();

        assertThat(list.size(), is(2));
        assertThat(list, hasItem(q1));
        assertThat(list, hasItem(a1));
    }

    @Test
    public void testListException() {
        addressApi.throwException = true;
        Response response = invoke(() -> addressService.getAddressList(securityContext,"myspace"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        Response response = invoke(() -> addressService.getAddress(securityContext,"myspace", "q1"));
        assertThat(response.getStatus(), is(200));
        Address address = (Address) response.getEntity();

        assertThat(address, is(q1));
    }

    @Test
    public void testGetException() {
        addressApi.throwException = true;
        Response response = invoke(() -> addressService.getAddress(securityContext,"myspace", "q1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = invoke(() -> addressService.getAddress(securityContext,"unknownspace", "q1"));
        assertThat(response.getStatus(), is(404));

        response = invoke(() -> addressService.getAddress(securityContext,"myspace", "doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }


    @Test
    public void testCreate() {
        Address a2 = new Address.Builder()
                .setAddress("a2")
                .setType("anycast")
                .setPlan("plan1")
                .setAddressSpace("myspace")
                .build();
        AddressList list = new AddressList();
        list.add(a2);
        Response response = invoke(() -> addressService.appendAddress(securityContext,"myspace", Either.createRight(list)));
        assertThat(response.getStatus(), is(200));

        assertThat(addressApi.listAddresses(), hasItem(a2));
    }

    @Test
    public void testCreateException() {
        addressApi.throwException = true;
        Address a2 = new Address.Builder()
                .setAddress("a2")
                .setPlan("plan1")
                .setType("anycast")
                .build();
        AddressList list = new AddressList();
        list.add(a2);
        Response response = invoke(() -> addressService.appendAddress(securityContext,"myspace", Either.createRight(list)));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {
        Response response = invoke(() -> addressService.deleteAddress(securityContext,"myspace", "a1"));
        assertThat(response.getStatus(), is(200));

        assertThat(addressApi.listAddresses(), hasItem(q1));
        assertThat(addressApi.listAddresses().size(), is(1));
    }

    @Test
    public void testDeleteException() {
        addressApi.throwException = true;
        Response response = invoke(() -> addressService.deleteAddress(securityContext,"myspace", "a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testUnauthorized() {
        when(securityContext.isUserInRole(any())).thenReturn(false);
        Response response = invoke(() -> addressService.deleteAddress(securityContext,"myspace", "a1"));
        assertThat(response.getStatus(), is(401));

        response = invoke(() -> addressService.getAddressList(securityContext,"myspace"));
        assertThat(response.getStatus(), is(401));

        response = invoke(() -> addressService.getAddress(securityContext,"myspace", "q1"));
        assertThat(response.getStatus(), is(401));

        Address a2 = new Address.Builder()
                .setAddress("a2")
                .setType("anycast")
                .setPlan("plan1")
                .setAddressSpace("myspace")
                .build();
        AddressList list = new AddressList();
        list.add(a2);
        response = invoke(() -> addressService.appendAddress(securityContext,"myspace", Either.createRight(list)));
        assertThat(response.getStatus(), is(401));
    }
}
