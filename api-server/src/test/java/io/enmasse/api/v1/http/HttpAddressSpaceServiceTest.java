/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.EndpointSpecBuilder;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.common.Status;
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
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
    private AuthenticationServiceRegistry authenticationServiceRegistry;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        authenticationServiceRegistry = mock(AuthenticationServiceRegistry.class);
        AuthenticationService authenticationService = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName("standard")
                .endMetadata()
                .withNewStatus()
                .withHost("example")
                .withPort(5671)
                .endStatus()
                .build();
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(authenticationService));
        when(authenticationServiceRegistry.resolveDefaultAuthenticationService()).thenReturn(Optional.of(authenticationService));
        addressSpaceService = new HttpAddressSpaceService(addressSpaceApi, new TestSchemaProvider(), Clock.systemUTC(), authenticationServiceRegistry);
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);
        a1 = new AddressSpaceBuilder()

                .withNewMetadata()
                .withNamespace("myns")
                .withName("a1")
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
                .withNamespace("myns")
                .withName("a2")
                .withCreationTimestamp(TimeUtil.formatRfc3339(Instant.ofEpochSecond(12)))
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

//    @Test
//    public void testList() {
//        addressSpaceApi.createAddressSpace(a1);
//        addressSpaceApi.createAddressSpace(a2);
//        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext, MediaType.APPLICATION_JSON, "myns", null, false, null, null));
//        assertThat(response.getStatus(), is(200));
//        AddressSpaceList data = (AddressSpaceList) response.getEntity();
//
//        assertThat(data.getItems().size(), is(2));
//        assertThat(data.getItems(), hasItem(a1));
//        assertThat(data.getItems(), hasItem(a2));
//    }
//
//    @Test
//    public void testListTableFormat() {
//        addressSpaceApi.createAddressSpace(a1);
//        addressSpaceApi.createAddressSpace(a2);
//        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "myns", null, false, null, null));
//        assertThat(response.getStatus(), is(200));
//        Table data = (Table) response.getEntity();
//
//        assertThat(data.getColumnDefinitions().size(), is(7));
//        assertThat(data.getRows().size(), is(2));
//    }
//
//    @Test
//    public void testListException() {
//        addressSpaceApi.throwException = true;
//        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext, MediaType.APPLICATION_JSON, "myns", null, false, null));
//        assertThat(response.getStatus(), is(500));
//    }

    @Test
    public void testGet() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.getAddressSpace(securityContext, null, "myns", "a1"));
        assertThat(response.getStatus(), is(200));
        AddressSpace data = ((AddressSpace) response.getEntity());

        assertThat(data, is(a1));
        assertThat(data.getSpec().getEndpoints().size(), is(a1.getSpec().getEndpoints().size()));
    }

    @Test
    public void testGetTableFormat() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.getAddressSpace(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "myns", "a1"));
        assertThat(response.getStatus(), is(200));
        Table data = ((Table) response.getEntity());

        assertThat(data.getColumnDefinitions().size(), is(7));
        assertThat(data.getRows().get(0).getObject().getMetadata().getName(), is(a1.getMetadata().getName()));
    }

    @Test
    public void testPut() {
        addressSpaceApi.createAddressSpace(a1);
        AddressSpace a1Adapted = new AddressSpaceBuilder(a1).editOrNewMetadata().addToAnnotations("foo", "bar").endMetadata().build();
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, "myns", a1Adapted.getMetadata().getName(), a1Adapted));
        assertThat(response.getStatus(), is(200));

        assertFalse(addressSpaceApi.listAddressSpaces("myns").isEmpty());
        assertThat(addressSpaceApi.listAddressSpaces("myns").iterator().next().getAnnotation("foo"), is("bar"));
    }

    @Test
    public void testPutChangeType() {
        addressSpaceApi.createAddressSpace(a1);
        AddressSpace a1Adapted = new AddressSpaceBuilder(a1).editOrNewSpec().withType("type2").endSpec().build();
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, "myns", a1Adapted.getMetadata().getName(), a1Adapted));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutChangePlan() {
        addressSpaceApi.createAddressSpace(a1);
        AddressSpace a1Adapted = new AddressSpaceBuilder(a1).editOrNewSpec().withPlan("otherplan").endSpec().build();
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, "myns", a1Adapted.getMetadata().getName(), a1Adapted));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNonMatchingAddressSpaceName() {
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, a1.getMetadata().getNamespace(), "xxxxxx", a1));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNonExistingAddressSpace() {
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, a1.getMetadata().getNamespace(), a1.getMetadata().getName(), a1));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testDelete() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);

        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext, "myns", "a1"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));

        assertThat(addressSpaceApi.listAddressSpaces("myns"), hasItem(a2));
        assertThat(addressSpaceApi.listAddressSpaces("myns").size(), is(1));
    }

    @Test
    public void testCreateUnresolvableDefaultAuthenticationService() {
        when(authenticationServiceRegistry.resolveDefaultAuthenticationService()).thenReturn(Optional.empty());
        UriInfo mockUriInfo = mock(UriInfo.class);
        Response response = invoke(() -> addressSpaceService.createAddressSpace(securityContext, mockUriInfo, a1.getMetadata().getNamespace(), a1));
        assertThat(response.getStatus(), is(500));
        Status body = (Status) response.getEntity();
        assertTrue(body.getMessage().contains("no authentication services found"));
    }

    @Test
    public void testCreateUnresolvableAuthenticationService() {
        when(authenticationServiceRegistry.findAuthenticationServiceByName(any())).thenReturn(Optional.empty());
        UriInfo mockUriInfo = mock(UriInfo.class);
        AddressSpace space = new AddressSpaceBuilder()

                .withNewMetadata()
                .withNamespace("myns")
                .withName("a1")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .editOrNewAuthenticationService()
                .withName("unknown")
                .endAuthenticationService()
                .endSpec()

                .build();

        Response response = invoke(() -> addressSpaceService.createAddressSpace(securityContext, mockUriInfo, space.getMetadata().getNamespace(), space));
        assertThat(response.getStatus(), is(400));
        Status body = (Status) response.getEntity();
        assertTrue(body.getMessage().contains("Unable to find authentication service"));
    }

    @Test
    public void testDeleteException() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext, "myns", "a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDeleteNotFound() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext, "myns", "doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testDeleteAll() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);

        Response response = invoke(() -> addressSpaceService.deleteAddressSpaces(securityContext, "myns"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));
    }

    @Test
    public void jsonPatch_RFC6902() throws Exception {
        addressSpaceApi.createAddressSpace(a1);

        final JsonPatch patch = mapper.readValue("[{\"op\":\"add\",\"path\":\"/metadata/annotations/fish\",\"value\":\"dorado\"}]", JsonPatch.class);

        Response response = addressSpaceService.patchAddressSpace(securityContext, a1.getMetadata().getNamespace(), a1.getMetadata().getName(), patch);
        assertThat(response.getStatus(), is(200));
        assertThat(((AddressSpace) response.getEntity()).getAnnotation("fish"), is("dorado"));
    }

    @Test
    public void jsonMergePatch_RFC7386() throws Exception {
        addressSpaceApi.createAddressSpace(a1);

        final JsonMergePatch mergePatch = mapper.readValue("{\"metadata\":{\"annotations\":{\"fish\":\"dorado\"}}}\n", JsonMergePatch.class);

        Response response = addressSpaceService.patchAddressSpace(securityContext, a1.getMetadata().getNamespace(), a1.getMetadata().getName(), mergePatch);
        assertThat(response.getStatus(), is(200));
        assertThat(((AddressSpace) response.getEntity()).getAnnotation("fish"), is("dorado"));
    }

    @Test
    public void patchAddressSpaceNotFound() throws Exception {
        final JsonPatch patch = mapper.readValue("[{\"op\":\"add\",\"path\":\"/metadata/annotations/fish\",\"value\":\"dorado\"}]", JsonPatch.class);

        Response response = addressSpaceService.patchAddressSpace(securityContext, "myns", "unknown", patch);
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void patchImmutable() throws Exception {
        addressSpaceApi.createAddressSpace(a1);

        final JsonPatch patch = mapper.readValue("[" +
                "{\"op\":\"replace\",\"path\":\"/metadata/name\",\"value\":\"newname\"}," +
                "{\"op\":\"replace\",\"path\":\"/metadata/namespace\",\"value\":\"newnamespace\"}" +
                "]", JsonPatch.class);

        Response response = addressSpaceService.patchAddressSpace(securityContext, a1.getMetadata().getNamespace(), a1.getMetadata().getName(), patch);
        // Should we reject a patch contain a name/namespace change etc?
        assertThat(response.getStatus(), is(200));
        AddressSpace updated = (AddressSpace) response.getEntity();
        assertThat(updated.getMetadata().getName(), is(a1.getMetadata().getName()));
        assertThat(updated.getMetadata().getNamespace(), is(a1.getMetadata().getNamespace()));
    }
}
