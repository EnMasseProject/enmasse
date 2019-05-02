/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.v1.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.EndpointSpecBuilder;
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.util.TimeUtil;

public class HttpAddressServiceTest {

    private static final String MY_NAMESPACE = "myns";
    private static final String MY_ADDRESSSPACE = "myspace";
    private final ObjectMapper mapper = new ObjectMapper();
    private final AddressSpaceApi addressSpaceApi = new TestAddressSpaceApi();
    private final HttpAddressService addressService = new HttpAddressService(addressSpaceApi, new TestSchemaProvider(), Clock.systemUTC());
    private final SecurityContext securityContext = mock(SecurityContext.class);
    private AddressSpace addressSpace;
    private AddressApi addressApi;
    private Address address1;

    @BeforeEach
    void setUp() throws Exception {
        when(securityContext.isUserInRole(any())).thenReturn(true);

        addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(MY_NAMESPACE)
                .withName(MY_ADDRESSSPACE)
                .withCreationTimestamp(TimeUtil.formatRfc3339(Instant.ofEpochSecond(123)))
                .endMetadata()
                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .withEndpoints(Collections.singletonList(
                        new EndpointSpecBuilder()
                                .withName("messaging")
                                .withService("messaging")
                                .build()))
                .endSpec()
                .build();

        addressSpaceApi.createAddressSpace(addressSpace);
        addressApi = addressSpaceApi.withAddressSpace(addressSpace);

        address1 = new AddressBuilder()
                .withNewMetadata()
                .withName(String.format("%s.q1", MY_ADDRESSSPACE))
                .withNamespace(MY_NAMESPACE)
                .endMetadata()

                .withNewSpec()
                .withAddressSpace(addressSpace.getMetadata().getName())
                .withAddress("q1")
                .withType("queue")
                .withPlan("plan1")
                .endSpec()
                .build();
    }

    @Test
    public void get() throws Exception {
        addressApi.createAddress(address1);
        Response response = addressService.getAddress(securityContext, null, MY_NAMESPACE, address1.getMetadata().getName());
        assertThat(response.getStatus(), is(200));
        Address data = ((Address) response.getEntity());
        assertThat(data, is(address1));
    }

    @Test
    public void getNotFound() throws Exception {
        Response response = addressService.getAddress(securityContext, null, MY_NAMESPACE, address1.getMetadata().getName());
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void jsonPatch_RFC6902() throws Exception {
        addressApi.createAddress(address1);

        final JsonPatch patch = mapper.readValue("[{\"op\":\"add\",\"path\":\"/metadata/annotations/fish\",\"value\":\"dorado\"}]", JsonPatch.class);

        Response response = addressService.patchAddress(securityContext, address1.getMetadata().getNamespace(), address1.getMetadata().getName(), patch);
        assertThat(response.getStatus(), is(200));
        assertThat(((Address) response.getEntity()).getAnnotation("fish"), is("dorado"));
    }


    @Test
    public void jsonMergePatch_RFC7386() throws Exception {
        addressApi.createAddress(address1);

        final JsonMergePatch mergePatch = mapper.readValue("{\"metadata\":{\"annotations\":{\"fish\":\"dorado\"}}}\n", JsonMergePatch.class);

        Response response = addressService.patchAddress(securityContext, address1.getMetadata().getNamespace(), address1.getMetadata().getName(), mergePatch);
        assertThat(response.getStatus(), is(200));
        assertThat(((Address) response.getEntity()).getAnnotation("fish"), is("dorado"));
    }

    @Test
    public void patchAddressSpaceNotFound() throws Exception {
        final JsonPatch patch = mapper.readValue("[{\"op\":\"add\",\"path\":\"/metadata/annotations/fish\",\"value\":\"dorado\"}]", JsonPatch.class);

        Response response = addressService.patchAddress(securityContext, MY_NAMESPACE, address1.getMetadata().getName(), patch);
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void patchImmutableIgnored() throws Exception {
        addressApi.createAddress(address1);

        final JsonPatch patch = mapper.readValue("[" +
                "{\"op\":\"replace\",\"path\":\"/metadata/name\",\"value\":\"newname\"}," +
                "{\"op\":\"replace\",\"path\":\"/metadata/namespace\",\"value\":\"newnamespace\"}," +
                "{\"op\":\"add\",\"path\":\"/metadata/labels/mylabel\",\"value\":\"myvalue\"}" +
                "]", JsonPatch.class);

        Response response = addressService.patchAddress(securityContext, address1.getMetadata().getNamespace(), address1.getMetadata().getName(), patch);
        assertThat(response.getStatus(), is(200));
        Address updated = (Address) response.getEntity();
        assertThat(updated.getMetadata().getLabels().get("mylabel"), is("myvalue"));
        assertThat(updated.getMetadata().getName(), is(address1.getMetadata().getName()));
        assertThat(updated.getMetadata().getNamespace(), is(address1.getMetadata().getNamespace()));
    }
}
