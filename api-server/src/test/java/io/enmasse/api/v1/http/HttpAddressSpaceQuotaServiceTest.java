/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.v1.quota.*;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.TestAddressSpaceQuotaApi;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpAddressSpaceQuotaServiceTest {
    private HttpAddressSpaceQuotaService quotaService;
    private TestAddressSpaceQuotaApi testQuotaApi;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @Before
    public void setup() {
        testQuotaApi = new TestAddressSpaceQuotaApi();
        quotaService = new HttpAddressSpaceQuotaService(testQuotaApi, new TestSchemaProvider());
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);

        testQuotaApi.createAddressSpaceQuota(new AddressSpaceQuota(
                new AddressSpaceQuotaMetadata("myquota", null, null),
                new AddressSpaceQuotaSpec("developer", Arrays.asList(
                        new AddressSpaceQuotaRule(1, "standard", "unlimited-standard"),
                        new AddressSpaceQuotaRule(2, "brokered", "unlimited-brokered")))));

        testQuotaApi.createAddressSpaceQuota(new AddressSpaceQuota(
                new AddressSpaceQuotaMetadata("secondquota", Collections.singletonMap("key1", "value1"), null),
                new AddressSpaceQuotaSpec("otheruser", Arrays.asList(
                        new AddressSpaceQuotaRule(3, "standard", "unlimited-standard")))));
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
        Response response = invoke(() -> quotaService.getAddressSpaceQuotaList(securityContext, null));
        assertThat(response.getStatus(), is(200));
        AddressSpaceQuotaList data = (AddressSpaceQuotaList) response.getEntity();

        assertThat(data.getItems().size(), is(2));
    }

    @Test
    public void testListException() {
        testQuotaApi.throwException = new RuntimeException("foo");
        Response response = invoke(() -> quotaService.getAddressSpaceQuotaList(securityContext, null));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        Response response = invoke(() -> quotaService.getAddressSpaceQuota(securityContext, "myquota"));
        assertThat(response.getStatus(), is(200));
        AddressSpaceQuota quota = ((AddressSpaceQuota)response.getEntity());

        assertEquals("myquota", quota.getMetadata().getName());
        assertEquals("developer", quota.getSpec().getUser());
        assertEquals(2, quota.getSpec().getRules().size());
    }

    @Test
    public void testGetException() {
        testQuotaApi.throwException = new RuntimeException("foo");
        Response response = invoke(() -> quotaService.getAddressSpaceQuota(securityContext, "myquota"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = invoke(() -> quotaService.getAddressSpaceQuota(securityContext, "doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testCreate() {
        Response response = invoke(() -> quotaService.createAddressSpaceQuota(securityContext, new ResteasyUriInfo("https://localhost:8443/foo", null, "/"),

                new AddressSpaceQuota(
                        new AddressSpaceQuotaMetadata("thirdquota", null, null),
                        new AddressSpaceQuotaSpec("developer", Arrays.asList(
                                new AddressSpaceQuotaRule(3, "standard", "unlimited-standard"))))));
        assertThat(response.getStatus(), is(201));

        assertThat(testQuotaApi.listAddressSpaceQuotas().getItems().size(), is(3));
    }

    @Test
    public void testCreateException() {
        testQuotaApi.throwException = new RuntimeException("foo");
        Response response = invoke(() -> quotaService.createAddressSpaceQuota(securityContext, new ResteasyUriInfo("https://localhost:8443/foo", null, "/"),

                new AddressSpaceQuota(
                        new AddressSpaceQuotaMetadata("thirdquota", null, null),
                        new AddressSpaceQuotaSpec("developer", Arrays.asList(
                                new AddressSpaceQuotaRule(3, "standard", "unlimited-standard"))))));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {
        Response response = invoke(() -> quotaService.deleteAddressSpaceQuota(securityContext, "myquota"));
        assertThat(response.getStatus(), is(200));

        assertThat(testQuotaApi.listAddressSpaceQuotas().getItems().size(), is(1));
    }

    @Test
    public void testDeleteException() {
        testQuotaApi.throwException = new RuntimeException("foo");
        Response response = invoke(() -> quotaService.deleteAddressSpaceQuota(securityContext, "myquota"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDeleteNotFound() {
        Response response = invoke(() -> quotaService.deleteAddressSpaceQuota(securityContext, "doeosnotexist"));
        assertThat(response.getStatus(), is(404));
    }
}
