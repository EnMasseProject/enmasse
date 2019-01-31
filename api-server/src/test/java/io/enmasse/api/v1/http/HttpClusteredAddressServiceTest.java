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
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.TestAddressApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpClusteredAddressServiceTest {
    private HttpClusterAddressService addressService;
    private TestAddressSpaceApi addressSpaceApi;

    private AddressSpace addressSpace1;
    private AddressSpace addressSpace2;
    private Address a1;
    private Address a2;

    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @BeforeEach
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        this.addressService = new HttpClusterAddressService(addressSpaceApi, new TestSchemaProvider(), Clock.fixed(Instant.ofEpochSecond(1234), ZoneId.of("UTC")));
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);

        this.addressSpace1 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("ns1")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();

        this.addressSpace2 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("ns2")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();

        this.a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .withNamespace("ns1")
                .endMetadata()

                .withNewSpec()
                .withAddress("A1")
                .withAddressSpace("myspace")
                .withType("anycast")
                .endSpec()

                .build();

        this.a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .withNamespace("ns2")
                .endMetadata()

                .withNewSpec()
                .withAddress("A1")
                .withAddressSpace("myspace")
                .withType("anycast")
                .endSpec()

                .build();

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

        addressSpaceApi.createAddressSpace(addressSpace1);
        addressSpaceApi.createAddressSpace(addressSpace2);

        TestAddressApi addressApi1 = (TestAddressApi) addressSpaceApi.withAddressSpace(addressSpace1);
        TestAddressApi addressApi2 = (TestAddressApi) addressSpaceApi.withAddressSpace(addressSpace2);

        addressApi1.createAddress(a1);
        addressApi2.createAddress(a2);

        Response response = invoke(() -> addressService.getAddressList(securityContext, null, null, null));

        assertThat(response.getStatus(), is(200));
        AddressList list = (AddressList) response.getEntity();

        assertThat(list.getItems().size(), is(2));
        assertThat(list.getItems(), hasItems(a1, a2));
    }

}
