/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.v1.quota.*;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.quota.AddressSpaceQuotaReviewer;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.AddressSpaceQuotaApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
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
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpAddressSpaceQuotaReviewServiceTest {
    private HttpAddressSpaceQuotaReviewService quotaReviewService;
    private AddressSpaceApi addressSpaceApi;
    private TestAddressSpaceQuotaApi addressSpaceQuotaApi;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @Before
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        addressSpaceQuotaApi = new TestAddressSpaceQuotaApi();

        AddressSpaceQuotaReviewer reviewer = new AddressSpaceQuotaReviewer(addressSpaceQuotaApi, addressSpaceApi);
        quotaReviewService = new HttpAddressSpaceQuotaReviewService(reviewer);
        addressSpaceQuotaApi.createAddressSpaceQuota(TestAddressSpaceQuotaApi.createQuota("quota1", "user1", new AddressSpaceQuotaRule(2, "standard", "unlimited-standard")));
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);
    }

    private Response invoke(Callable<Response> fn) {
        try {
            return fn.call();
        } catch (Exception e) {
            return exceptionMapper.toResponse(e);
        }
    }

    @Test
    public void testCreate() {
        Response response = invoke(() -> quotaReviewService.createAddressSpaceQuotaReview(securityContext,
                new AddressSpaceQuotaReview( new AddressSpaceQuotaReviewSpec("user1", null), null)));
        assertThat(response.getStatus(), is(200));

        AddressSpaceQuotaReview result = (AddressSpaceQuotaReview) response.getEntity();
        assertFalse(result.getStatus().isExceeded());
    }

    @Test
    public void testCreateExceededQuota() throws Exception {
        createAddressSpace("myspace");
        createAddressSpace("myspace2");
        createAddressSpace("myspace3");
        Response response = invoke(() -> quotaReviewService.createAddressSpaceQuotaReview(securityContext,
                new AddressSpaceQuotaReview( new AddressSpaceQuotaReviewSpec("user1", null), null)));
        assertThat(response.getStatus(), is(200));

        AddressSpaceQuotaReview result = (AddressSpaceQuotaReview) response.getEntity();
        assertTrue(result.getStatus().isExceeded());
    }

    private void createAddressSpace(String name) throws Exception {
        addressSpaceApi.createAddressSpace(new AddressSpace.Builder()
                .setName(name)
                .setNamespace("ns")
                .putLabel(LabelKeys.CREATED_BY, "user1")
                .setType("standard")
                .setPlan("unlimited-standard")
                .build());

    }
}
