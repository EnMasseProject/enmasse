/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.EndpointSpecBuilder;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.model.v1beta1.Table;
import io.enmasse.k8s.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class HttpClusterAddressSpaceServiceTest {
    private HttpClusterAddressSpaceService addressSpaceService;
    private TestAddressSpaceApi addressSpaceApi;
    private AddressSpace a1;
    private AddressSpace a2;
    private SecurityContext securityContext;
    private AsyncResponse asyncResponse;

    private ArgumentCaptor<Response> responseArgumentCaptor = ArgumentCaptor.forClass(Response.class);
    private ArgumentCaptor<Exception> exceptionArgumentCaptor = ArgumentCaptor.forClass(Exception.class);

    @BeforeEach
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        addressSpaceService = new HttpClusterAddressSpaceService(addressSpaceApi, Clock.systemUTC());
        securityContext = mock(SecurityContext.class);
        asyncResponse = mock(AsyncResponse.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);

        a1 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("a1")
                .withNamespace("myns")
                .withCreationTimestamp(TimeUtil.formatRfc3339(Instant.ofEpochSecond(123)))
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")

                .withEndpoints(Arrays.asList(
                        new EndpointSpecBuilder()
                                .withName("messaging")
                                .withService("messaging")
                                .build(),
                        new EndpointSpecBuilder()
                                .withName("mqtt")
                                .withService("mqtt")
                                .build()))

                .endSpec()

                .build();

        a2 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("a2")
                .withNamespace("othernamespace")
                .withCreationTimestamp(TimeUtil.formatRfc3339(Instant.ofEpochSecond(12)))
                .endMetadata()


                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();
    }


    @Test
    public void testList() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);
        addressSpaceService.getAddressSpaceList(securityContext, MediaType.APPLICATION_JSON, null, false, null, asyncResponse);
        verify(asyncResponse).resume(this.responseArgumentCaptor.capture());
        final Response response = this.responseArgumentCaptor.getValue();
        assertThat(response.getStatus(), is(200));
        AddressSpaceList data = (AddressSpaceList) response.getEntity();

        assertNotNull(data.getItems());
        assertThat(data.getItems().size(), is(2));
        assertThat(data.getItems(), hasItem(a1));
        assertThat(data.getItems(), hasItem(a2));
    }

    @Test
    public void testListTableFormat() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);
        addressSpaceService.getAddressSpaceList(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", null, false, null, asyncResponse);
        verify(asyncResponse).resume(this.responseArgumentCaptor.capture());
        final Response response = this.responseArgumentCaptor.getValue();
        assertThat(response.getStatus(), is(200));
        Table data = (Table) response.getEntity();

        assertThat(data.getColumnDefinitions().size(), is(7));
        assertThat(data.getRows().size(), is(2));
    }

    @Test
    public void testListException() {
        addressSpaceApi.throwException = true;
        addressSpaceService.getAddressSpaceList(securityContext, MediaType.APPLICATION_JSON, null, false, null, asyncResponse);
        verify(asyncResponse).resume(this.exceptionArgumentCaptor.capture());
        final Exception response = this.exceptionArgumentCaptor.getValue();

        assertThat(response, instanceOf(RuntimeException.class));
    }
}
