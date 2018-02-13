/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.Endpoint;
import io.enmasse.controller.api.DefaultExceptionMapper;
import io.enmasse.k8s.api.SchemaApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.api.TestSchemaApi;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpAddressSpaceServiceTest {
    private HttpAddressSpaceService addressSpaceService;
    private TestAddressSpaceApi addressSpaceApi;
    private AddressSpace a1;
    private AddressSpace a2;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @Before
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        SchemaApi schemaApi = new TestSchemaApi();
        addressSpaceService = new HttpAddressSpaceService(addressSpaceApi, schemaApi,"controller");
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(new BasicUserPrincipal("me"));
        when(securityContext.isUserInRole(any())).thenReturn(true);
        when(securityContext.getAuthenticationScheme()).thenReturn("unknown");
        a1 = new AddressSpace.Builder()
                .setName("a1")
                .setNamespace("myspace")
                .setType("type1")
                .setPlan("myplan")
                .setEndpointList(Arrays.asList(
                        new Endpoint.Builder()
                            .setName("messaging")
                            .setService("messaging")
                            .setHost("messaging.example.com")
                        .build(),
                        new Endpoint.Builder()
                            .setName("mqtt")
                            .setService("mqtt")
                            .setHost("mqtt.example.com")
                        .build()))
                .build();

        a2 = new AddressSpace.Builder()
                .setName("a2")
                .setType("type1")
                .setPlan("myplan")
                .setNamespace("othernamespace")
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
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext));
        assertThat(response.getStatus(), is(200));
        AddressSpaceList data = (AddressSpaceList) response.getEntity();

        assertThat(data.size(), is(2));
        assertThat(data, hasItem(a1));
        assertThat(data, hasItem(a2));
    }

    @Test
    public void testListException() {
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.getAddressSpace(securityContext,"a1"));
        assertThat(response.getStatus(), is(200));
        AddressSpace data = ((AddressSpace)response.getEntity());

        assertThat(data, is(a1));
        assertThat(data.getEndpoints().size(), is(a1.getEndpoints().size()));
    }

    @Test
    public void testGetException() {
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.getAddressSpace(securityContext,"a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = invoke(() -> addressSpaceService.getAddressSpace(securityContext,"doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testCreate() {
        Response response = invoke(() -> addressSpaceService.createAddressSpace(securityContext, a1));
        assertThat(response.getStatus(), is(200));

        assertThat(addressSpaceApi.listAddressSpaces(), hasItem(a1));
    }

    @Test
    public void testCreateException() {
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.createAddressSpace(securityContext, a1));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);

        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext,"a1"));
        assertThat(response.getStatus(), is(200));

        assertThat(addressSpaceApi.listAddressSpaces(), hasItem(a2));
        assertThat(addressSpaceApi.listAddressSpaces().size(), is(1));
    }

    @Test
    public void testDeleteException() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext,"a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDeleteNotFound() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext,"doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testUnauthorized() {
        when(securityContext.isUserInRole(any())).thenReturn(false);

        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext,"doesnotexist"));
        assertThat(response.getStatus(), is(401));

        addressSpaceApi.createAddressSpace(a1);
        response = invoke(() -> addressSpaceService.getAddressSpace(securityContext,"a1"));
        assertThat(response.getStatus(), is(401));

        response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext));
        assertThat(response.getStatus(), is(401));

        response = invoke(() -> addressSpaceService.createAddressSpace(securityContext, a1));
        assertThat(response.getStatus(), is(401));
    }
}
