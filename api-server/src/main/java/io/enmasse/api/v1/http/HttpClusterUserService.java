/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.UserCrd;
import io.enmasse.user.model.v1.UserList;

import static io.enmasse.api.v1.http.HttpUserService.parseLabelSelector;

@Path(HttpClusterUserService.BASE_URI)
public class HttpClusterUserService {

    private static final String RESOURCE_NAME = "messagingusers";

    static final String BASE_URI = "/apis/" + UserCrd.GROUP + "/{version:v1alpha1|v1beta1}/" + RESOURCE_NAME;

    private static final Logger log = LoggerFactory.getLogger(HttpClusterUserService.class.getName());

    private final UserApi userApi;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private final Clock clock;

    public HttpClusterUserService(UserApi userApi, AuthenticationServiceRegistry authenticationServiceRegistry, Clock clock) {
        this.userApi = userApi;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
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
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole("", verb, RESOURCE_NAME, UserCrd.GROUP))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUserList(@Context SecurityContext securityContext, @HeaderParam("Accept") String acceptHeader, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return doRequest("Error getting user list", () -> {
            verifyAuthorized(securityContext, ResourceVerb.list);

            Instant now = clock.instant();
            UserList userList = new UserList();

            for (AuthenticationService authenticationService : authenticationServiceRegistry.listAuthenticationServices()) {
                if (labelSelector != null) {
                    Map<String, String> labels = parseLabelSelector(labelSelector);
                    userList.getItems().addAll(userApi.listAllUsersWithLabels(authenticationService, labels).getItems());
                } else {
                    userList.getItems().addAll(userApi.listAllUsers(authenticationService).getItems());
                }
            }

            return Response.ok(HttpUserService.formatResponse(acceptHeader, now, userList)).build();
        });
    }
}
