/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.EndpointSpecBuilder;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.common.Status;
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.model.v1beta1.Table;
import io.enmasse.k8s.util.TimeUtil;

public class HttpAddressSpaceServiceTest {
    private HttpAddressSpaceService addressSpaceService;
    private TestAddressSpaceApi addressSpaceApi;
    private AddressSpace a1;
    private AddressSpace a2;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @BeforeEach
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        addressSpaceService = new HttpAddressSpaceService(addressSpaceApi, new TestSchemaProvider(), Clock.systemUTC());
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);
        a1 = new AddressSpaceBuilder()

                .withNewMetadata()
                .withName("a1")
                .withCreationTimestamp(TimeUtil.formatRfc3339(Instant.ofEpochSecond(123)))
                .withNamespace("myspace")
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
                .withCreationTimestamp(TimeUtil.formatRfc3339(Instant.ofEpochSecond(12)))
                .withNamespace("othernamespace")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
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
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext, MediaType.APPLICATION_JSON, null, null));
        assertThat(response.getStatus(), is(200));
        AddressSpaceList data = (AddressSpaceList) response.getEntity();

        assertThat(data.getItems().size(), is(2));
        assertThat(data.getItems(), hasItem(a1));
        assertThat(data.getItems(), hasItem(a2));
    }

    @Test
    public void testListTableFormat() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", null, null));
        assertThat(response.getStatus(), is(200));
        Table data = (Table) response.getEntity();

        assertThat(data.getColumnDefinitions().size(), is(6));
        assertThat(data.getRows().size(), is(2));
    }

    @Test
    public void testListException() {
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext, MediaType.APPLICATION_JSON, null, null));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.getAddressSpace(securityContext, null, null, "a1"));
        assertThat(response.getStatus(), is(200));
        AddressSpace data = ((AddressSpace) response.getEntity());

        assertThat(data, is(a1));
        assertThat(data.getSpec().getEndpoints().size(), is(a1.getSpec().getEndpoints().size()));
    }

    @Test
    public void testGetTableFormat() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.getAddressSpace(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", null, "a1"));
        assertThat(response.getStatus(), is(200));
        Table data = ((Table) response.getEntity());

        assertThat(data.getColumnDefinitions().size(), is(6));
        assertThat(data.getRows().get(0).getObject().getMetadata().getName(), is(a1.getMetadata().getName()));
    }

    @Test
    public void testPut() {
        addressSpaceApi.createAddressSpace(a1);
        AddressSpace a1Adapted = new AddressSpaceBuilder(a1).editOrNewMetadata().addToAnnotations("foo", "bar").endMetadata().build();
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, a1Adapted.getMetadata().getName(), a1Adapted));
        assertThat(response.getStatus(), is(200));

        assertFalse(addressSpaceApi.listAddressSpaces(null).isEmpty());
        assertThat(addressSpaceApi.listAddressSpaces(null).iterator().next().getAnnotation("foo"), is("bar"));
    }

    @Test
    public void testPutChangeType() {
        addressSpaceApi.createAddressSpace(a1);
        AddressSpace a1Adapted = new AddressSpaceBuilder(a1).editOrNewSpec().withType("type2").endSpec().build();
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, a1Adapted.getMetadata().getName(), a1Adapted));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutChangePlan() {
        addressSpaceApi.createAddressSpace(a1);
        AddressSpace a1Adapted = new AddressSpaceBuilder(a1).editOrNewSpec().withPlan("otherplan").endSpec().build();
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, a1Adapted.getMetadata().getName(), a1Adapted));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNonMatchingAddressSpaceName() {
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, "xxxxxx", a1));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNonExistingAddressSpace() {
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, a1.getMetadata().getName(), a1));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testDelete() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);

        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext, null, "a1"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));

        assertThat(addressSpaceApi.listAddressSpaces(null), hasItem(a2));
        assertThat(addressSpaceApi.listAddressSpaces(null).size(), is(1));
    }

    @Test
    public void testDeleteException() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext, null, "a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDeleteNotFound() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext, null, "doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testDeleteAll() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);

        Response response = invoke(() -> addressSpaceService.deleteAddressSpaces(securityContext, "myspace"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));
    }
}
