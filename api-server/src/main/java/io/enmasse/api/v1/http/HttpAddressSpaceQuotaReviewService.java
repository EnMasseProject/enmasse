/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.v1.quota.AddressSpaceQuotaReview;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.quota.AddressSpaceQuotaReviewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.concurrent.Callable;

@Path(HttpAddressSpaceQuotaReviewService.BASE_URI)
public class HttpAddressSpaceQuotaReviewService {

    static final String BASE_URI = "/apis/enmasse.io/v1alpha1/addressspacequotareviews";

    private static final Logger log = LoggerFactory.getLogger(HttpAddressSpaceQuotaReviewService.class.getName());

    private final AddressSpaceQuotaReviewer quotaReviewer;

    public HttpAddressSpaceQuotaReviewService(AddressSpaceQuotaReviewer quotaReviewer) {
        this.quotaReviewer = quotaReviewer;
    }

    private Response doRequest(String errorMessage, Callable<Response> request) throws Exception {
        try {
            return request.call();
        } catch (Exception e) {
            log.error(errorMessage, e);
            throw e;
        }
    }

    private static void verifyAuthorized(SecurityContext securityContext, ResourceVerb verb) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToClusterRole(verb, "addressspacequotareviews"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createAddressSpaceQuotaReview(@Context SecurityContext securityContext, @NotNull AddressSpaceQuotaReview input) throws Exception {
        return doRequest("Error creating address space quota review", () -> {
            verifyAuthorized(securityContext, ResourceVerb.create);

            AddressSpaceQuotaReview result = quotaReviewer.reviewQuota(input);
            return Response.ok(result).build();
        });
    }
}
