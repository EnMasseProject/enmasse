/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import static io.enmasse.api.v1.http.HttpAddressSpaceService.formatResponse;
import static io.enmasse.api.v1.http.HttpAddressSpaceService.getWatcher;
import static io.enmasse.api.v1.http.HttpAddressSpaceService.removeSecrets;


import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.GET;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;

import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.k8s.api.AddressSpaceApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


@Path(HttpClusterAddressSpaceService.BASE_URI)
public class HttpClusterAddressSpaceService {

    static final String BASE_URI = "/apis/enmasse.io/{version:v1alpha1|v1beta1}/addressspaces";

    private static final Logger log = LoggerFactory.getLogger(HttpClusterAddressSpaceService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    public void getAddressSpaceList(@Context SecurityContext securityContext,
                                    @HeaderParam("Accept") String acceptHeader,
                                    @QueryParam("labelSelector") String labelSelector,
                                    @QueryParam("watch") @DefaultValue("false") boolean watch,
                                    @QueryParam("resourceVersion") String resourceVersion,
                                    @Suspended AsyncResponse asyncResponse) {
        verifyAuthorized(securityContext, ResourceVerb.list);
        final Map<String, String> labels = (labelSelector != null) ? AddressApiHelper.parseLabelSelector(labelSelector) : null;

        if (!watch) {
            try {
                Response response = doRequest("Error getting address space list", () -> {
                    Instant now = clock.instant();
                    return Response.ok(formatResponse(acceptHeader, now, removeSecrets(addressSpaceApi.listAllAddressSpaces(labels)))).build();
                });
                asyncResponse.resume(response);
            } catch (Exception e) {
                asyncResponse.resume(e);
            }
        } else {
            ExecutorService e = Executors.newFixedThreadPool(1);

            e.execute(() -> {
                try {
                    asyncResponse.resume(new StreamingOutput() {
                        CompletableFuture<Void> closeFuture = new CompletableFuture<>();
                        @Override
                        public void write(OutputStream output) throws IOException {
                            addressSpaceApi.watch(getWatcher(output, closeFuture), null, resourceVersion, labels);

                            try {
                                closeFuture.get();
                            } catch (InterruptedException e1) {
                                Thread.currentThread().interrupt();
                            } catch (ExecutionException e1) {
                                if (e1.getCause() instanceof IOException) {
                                    throw ((IOException) e1.getCause());
                                } else {
                                    throw new RuntimeException(e1.getCause());
                                }
                            }
                        }
                    });
                } finally {
                    e.shutdown();
                }
            });
        }
    }
}
