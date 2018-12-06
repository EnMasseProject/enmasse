/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.*;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.common.Status;
import io.enmasse.api.common.UuidGenerator;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.k8s.model.v1beta1.PartialObjectMetadata;
import io.enmasse.k8s.model.v1beta1.Table;
import io.enmasse.k8s.model.v1beta1.TableColumnDefinition;
import io.enmasse.k8s.model.v1beta1.TableRow;
import io.enmasse.k8s.util.TimeUtil;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static io.enmasse.api.v1.http.HttpAddressSpaceService.formatResponse;
import static io.enmasse.api.v1.http.HttpAddressSpaceService.removeSecrets;

@Path(HttpClusterAddressSpaceService.BASE_URI)
public class HttpClusterAddressSpaceService {

    static final String BASE_URI = "/apis/enmasse.io/v1alpha1/addressspaces";

    private static final Logger log = LoggerFactory.getLogger(HttpClusterAddressSpaceService.class.getName());

    private final AddressSpaceApi addressSpaceApi;
    private final Clock clock;

    public HttpClusterAddressSpaceService(AddressSpaceApi addressSpaceApi, Clock clock) {
        this.addressSpaceApi = addressSpaceApi;
        this.clock = clock;
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
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole("", verb, "addressspaces", "enmasse.io"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddressSpaceList(@Context SecurityContext securityContext, @HeaderParam("Accept") String acceptHeader, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return doRequest("Error getting address space list", () -> {
            verifyAuthorized(securityContext, ResourceVerb.list);
            Instant now = clock.instant();
            if (labelSelector != null) {
                Map<String, String> labels = AddressApiHelper.parseLabelSelector(labelSelector);
                return Response.ok(formatResponse(acceptHeader, now, removeSecrets(addressSpaceApi.listAllAddressSpacesWithLabels(labels)))).build();
            } else {
                return Response.ok(formatResponse(acceptHeader, now, removeSecrets(addressSpaceApi.listAllAddressSpaces()))).build();
            }
        });
    }
}
