/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.v1.quota.AddressSpaceQuota;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressSpaceQuotaApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;
import java.util.concurrent.Callable;

@Path(HttpAddressSpaceQuotaService.BASE_URI)
public class HttpAddressSpaceQuotaService {

    static final String BASE_URI = "/apis/enmasse.io/v1alpha1/addressspacequotas";

    private static final Logger log = LoggerFactory.getLogger(HttpAddressSpaceQuotaService.class.getName());

    private final AddressSpaceQuotaApi quotaApi;

    public HttpAddressSpaceQuotaService(AddressSpaceQuotaApi quotaApi) {
        this.quotaApi = quotaApi;
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
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToClusterRole(verb, "addressspacequotas"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddressSpaceQuotaList(@Context SecurityContext securityContext, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return doRequest("Error getting address space list", () -> {
            verifyAuthorized(securityContext, ResourceVerb.list);
            if (labelSelector != null) {
                Map<String, String> labels = AddressApiHelper.parseLabelSelector(labelSelector);
                return Response.ok(quotaApi.listAddressSpaceQuotasWithLabels(labels)).build();
            } else {
                return Response.ok(quotaApi.listAddressSpaceQuotas()).build();
            }
        });
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpaceQuota}")
    public Response getAddressSpaceQuota(@Context SecurityContext securityContext, @PathParam("addressSpaceQuota") String addressSpaceQuotaName) throws Exception {
        return doRequest("Error getting address space quota " + addressSpaceQuotaName, () -> {
            verifyAuthorized(securityContext, ResourceVerb.get);
            return quotaApi.getAddressSpaceQuotaWithName(addressSpaceQuotaName)
                    .map(addressSpace -> Response.ok(addressSpace).build())
                    .orElseThrow(() -> new NotFoundException("Address space quota " + addressSpaceQuotaName + " not found"));
        });
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createAddressSpaceQuota(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @NotNull AddressSpaceQuota input) throws Exception {
        return doRequest("Error creating address space quota " + input.getMetadata().getName(), () -> {
            verifyAuthorized(securityContext, ResourceVerb.create);

            // TODO: Validate rules
            input.getMetadata().putLabel(LabelKeys.USER, input.getSpec().getUser());

            quotaApi.createAddressSpaceQuota(input);
            AddressSpaceQuota created = quotaApi.getAddressSpaceQuotaWithName(input.getMetadata().getName()).orElse(input);
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(created.getMetadata().getName());
            return Response.created(builder.build()).entity(created).build();
        });
    }


    @DELETE
    @Path("{addressSpace}")
    public Response deleteAddressSpaceQuota(@Context SecurityContext securityContext, @PathParam("addressSpaceQuota") String addressSpaceQuotaName) throws Exception {
        return doRequest("Error deleting address space quota " + addressSpaceQuotaName, () -> {
            verifyAuthorized(securityContext, ResourceVerb.delete);
            AddressSpaceQuota addressSpaceQuota = quotaApi.getAddressSpaceQuotaWithName(addressSpaceQuotaName)
                    .orElseThrow(() -> new NotFoundException("Unable to find address space quota " + addressSpaceQuotaName));
            quotaApi.deleteAddressSpaceQuota(addressSpaceQuotaName);
            return Response.ok().build();
        });
    }
}
