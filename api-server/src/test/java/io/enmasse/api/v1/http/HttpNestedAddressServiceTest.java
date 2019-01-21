/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.common.Status;
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.TestAddressApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.model.v1beta1.Table;

import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.Callable;

import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpNestedAddressServiceTest {
    private HttpNestedAddressService addressService;
    private TestAddressSpaceApi addressSpaceApi;
    private TestAddressApi addressApi;
    private Address q1;
    private Address a1;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @BeforeEach
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        this.addressService = new HttpNestedAddressService(addressSpaceApi, new TestSchemaProvider(), Clock.fixed(Instant.ofEpochSecond(1234), ZoneId.of("UTC")));
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();

        addressSpaceApi.createAddressSpace(addressSpace);
        addressApi = (TestAddressApi) addressSpaceApi.withAddressSpace(addressSpace);
        q1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q1")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("Q1")
                .withAddressSpace("myspace")
                .withType("queue")
                .endSpec()

                .build();
        a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("A1")
                .withAddressSpace("myspace")
                .withType("anycast")
                .endSpec()

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
        Response response = invoke(() -> addressService.getAddressList(securityContext, null, "ns", "myspace", null, null));

        assertThat(response.getStatus(), is(200));
        AddressList list = (AddressList) response.getEntity();

        assertThat(list.getItems().size(), is(2));
        assertThat(list.getItems(), hasItem(q1));
        assertThat(list.getItems(), hasItem(a1));
    }

    @Test
    public void testListTableFormat() {
        Response response = invoke(() -> addressService.getAddressList(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "ns", "myspace", null, null));

        assertThat(response.getStatus(), is(200));
        Table table = (Table) response.getEntity();

        assertThat(table.getColumnDefinitions().size(), is(9));
        assertThat(table.getRows().size(), is(2));
    }

    @Test
    public void testGetByAddress() {
        Response response = invoke(() -> addressService.getAddressList(securityContext, null, "ns", "myspace", "A1", null));

        assertThat(response.getStatus(), is(200));
        Address address = (Address) response.getEntity();

        assertThat(address, is(a1));
    }

    @Test
    public void testGetByAddressNotFound() {
        Response response = invoke(() -> addressService.getAddressList(securityContext, null, "ns", "myspace", "b1", null));

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testListException() {
        addressApi.throwException = true;
        Response response = invoke(() -> addressService.getAddressList(securityContext, null, "ns", "myspace", null, null));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        Response response = invoke(() -> addressService.getAddress(securityContext, null, "ns", "myspace", "myspace.q1"));
        assertThat(response.getStatus(), is(200));
        Address address = (Address) response.getEntity();

        assertThat(address, is(q1));
    }

    @Test
    public void testGetTableFormat() {
        Response response = invoke(() -> addressService.getAddress(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "ns", "myspace", "myspace.q1"));
        assertThat(response.getStatus(), is(200));
        Table table = (Table) response.getEntity();

        assertThat(table.getColumnDefinitions().size(), is(9));
        assertThat(table.getRows().get(0).getObject().getMetadata().getName(), is(q1.getMetadata().getName()));
    }

    @Test
    public void testGetException() {
        addressApi.throwException = true;
        Response response = invoke(() -> addressService.getAddress(securityContext, null, "ns", "myspace", "myspace.q1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = invoke(() -> addressService.getAddress(securityContext, null, "ns", "myspace", "myspace.doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testCreateSingle() {
        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()

                .withNewSpec()
                .withAddress("a2")
                .withType("anycast")
                .withPlan("plan1")
                .withAddressSpace("myspace")
                .endSpec()
                .build();
        Response response = invoke(() -> addressService.createAddress(securityContext, new ResteasyUriInfo("http://localhost:8443/", null, "/"), "ns", "myspace", a2));
        assertThat(response.getStatus(), is(201));

        Address a2ns = new AddressBuilder(a2)
                .editOrNewMetadata().withNamespace("ns").endMetadata()
                .build();
        assertThat(addressApi.listAddresses("ns"), hasItem(a2ns));
    }

    @Test
    public void testCreateList() {
        final Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("a1")
                .withType("anycast")
                .withPlan("plan1")
                .endSpec()
                .build();

        final Address a2 = new AddressBuilder(a1)
                .editOrNewMetadata()
                .withName("myspace.a2")
                .endMetadata()

                .editOrNewSpec()
                .withAddress("a2")
                .endSpec()
                .build();

        final AddressList list = new AddressList();
        list.getItems().add(a1);
        list.getItems().add(a2);

        final Response response = invoke(() -> {
            return this.addressService.createAddress(securityContext,
                    new ResteasyUriInfo("http://localhost:8443/", null, "/"),
                    "ns", "myspace", list);
            });
        assertThat(response.getStatus(), is(201));

        final Address a1ns = new AddressBuilder(a1)
                .editOrNewMetadata().withNamespace("ns").endMetadata()
                .build();
        final Address a2ns = new AddressBuilder(a2)
                .editOrNewMetadata().withNamespace("ns").endMetadata()
                .build();

        assertThat(addressApi.listAddresses("ns"), hasItems(a1ns, a2ns));

    }

    @Test
    public void testCreateException() {
        addressApi.throwException = true;
        Address a2 = new AddressBuilder()

                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()

                .withNewSpec()
                .withAddress("a2")
                .withPlan("plan1")
                .withType("anycast")
                .endSpec()
                .build();
        Response response = invoke(() -> addressService.createAddress(securityContext, null, "ns", "myspace", a2));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testPut() {
        Set<Address> addresses = addressApi.listAddresses("ns");
        assertThat(addresses.isEmpty(), is(false));
        Address address = addresses.iterator().next();
        Address a1 = new AddressBuilder(address).editOrNewSpec().withPlan("plan1").endSpec().build();

        Response response = invoke(() -> addressService.replaceAddress(securityContext, "ns", "myspace", a1.getMetadata().getName(), a1));
        assertThat(response.getStatus(), is(200));

        Address a2ns = new AddressBuilder(a1).editOrNewMetadata().withNamespace("ns").endMetadata().build();
        assertThat(addressApi.listAddresses("ns"), hasItem(a2ns));
    }

    @Test
    public void testPutNonMatchingAddressName() {
        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()

                .withNewSpec()
                .withAddress("a2")
                .withType("anycast")
                .withPlan("plan1")
                .withAddressSpace("myspace")
                .endSpec()

                .build();
        Response response = invoke(() -> addressService.replaceAddress(securityContext, "ns", "myspace", "myspace.xxxxxxx", a2));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNonExistingAddress() {
        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()

                .withNewSpec()
                .withAddress("a2")
                .withType("anycast")
                .withPlan("plan1")
                .endSpec()

                .build();
        Response response = invoke(() -> addressService.replaceAddress(securityContext, "ns", "myspace", a2.getMetadata().getName(), a2));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testDelete() {
        Response response = invoke(() -> addressService.deleteAddress(securityContext, "ns", "myspace", "myspace.a1"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));

        assertThat(addressApi.listAddresses("ns"), hasItem(q1));
        assertThat(addressApi.listAddresses("ns").size(), is(1));
    }

    @Test
    public void testDeleteException() {
        addressApi.throwException = true;
        Response response = invoke(() -> addressService.deleteAddress(securityContext, "ns", "myspace", "myspace.a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDeleteNotFound() {
        Response response = invoke(() -> addressService.deleteAddress(securityContext, "ns", "myspace", "myspace.notFound"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void deleteAllAddresses() {
        Response response = invoke(() -> addressService.internalDeleteAddresses(securityContext, "unknown"));
        assertThat(response.getStatus(), is(200));
        assertThat(addressApi.listAddresses("ns").size(), is(2));

        response = invoke(() -> addressService.internalDeleteAddresses(securityContext, "ns"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));
        assertThat(addressApi.listAddresses("ns").size(), is(0));
    }

    @Test
    public void testCreateAddress1 () {
        final Address address = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.address")
                .endMetadata()

                .withNewSpec()
                .withAddress("address")
                .withPlan("plan1")
                .withType("anycast")
                .endSpec()
                .build();

        testCreation(address, true);
    }

    @Test
    public void testCreateIllegalAddress1 () {
        final Address address = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.address")
                .endMetadata()

                .withNewSpec()
                // missing: .withAddress("address")
                .withPlan("plan1")
                .withType("anycast")
                .endSpec()
                .build();

        testCreation(address, false);
    }

    @Test
    public void testCreateIllegalAddress2 () {
        final Address address = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.address")
                .endMetadata()

                .withNewSpec()
                .withAddress("address")
                // missing: .withPlan("plan1")
                .withType("anycast")
                .endSpec()
                .build();

        testCreation(address, false);
    }

    @Test
    public void testCreateIllegalAddress3 () {
        final Address address = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.address")
                .endMetadata()

                .withNewSpec()
                .withAddress("address")
                .withPlan("plan1")
                // missing: .withType("anycast")
                .endSpec()
                .build();

        testCreation(address, false);
    }

    /**
     * Try to create and illegal address request, and assert that it fails properly.
     * @param address The illegal address to create.
     */
    private void testCreation(final Address address, boolean successful) {
        final UriInfo uriInfo = new ResteasyUriInfo("http://localhost:8443/", null, "/");
        final Response response = invoke(() -> addressService.internalCreateAddress(securityContext, uriInfo, "ns", "myspace", address));

        if ( successful ) {
            assertThat(response.getStatusInfo().getFamily(), is(SUCCESSFUL) );
        } else {
            assertThat(response.getStatusInfo().getFamily(), not(SUCCESSFUL) );
            assertThat(response.getStatusInfo().getFamily(), not(SERVER_ERROR));
        }
    }

    @Test
    public void testCreateWithDefaults1 () {
        final String addressName = "addresstestCreateWithDefaults1";

        final Address address = new AddressBuilder()
                .withNewMetadata()
                .withNamespace("ns")
                .withName("myspace." + addressName)
                .endMetadata()

                .withNewSpec()
                .withAddress(addressName)
                .withPlan("plan1")
                .withType("anycast")
                .endSpec()
                .build();

        final UriInfo uriInfo = new ResteasyUriInfo("http://localhost:8443/", null, "/");
        final Response response1 = invoke(() -> addressService.internalCreateAddress(securityContext, uriInfo, "ns", "myspace", address));

        assertThat(response1.getStatusInfo().getFamily(), is(SUCCESSFUL) );

        Response response2 = invoke(() -> addressService.getAddressList(securityContext, null, "ns", "myspace", addressName, null));

        assertThat(response2.getStatus(), is(200));
        Address addressFetched = (Address) response2.getEntity();

        assertThat(addressFetched.getMetadata(), notNullValue());
        assertThat(addressFetched.getMetadata().getName(), is("myspace." + addressName));
        assertThat(addressFetched.getMetadata().getNamespace(), is("ns"));

        assertThat(addressFetched.getSpec(), notNullValue());
        assertThat(addressFetched.getSpec().getAddress(), is(addressName));
        assertThat(Address.extractAddressSpace(addressFetched), is("myspace"));

    }
}
