/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;
import java.util.concurrent.Callable;

@Path(HttpUserService.BASE_URI)
public class HttpUserService {

    static final String BASE_URI = "/apis/user.enmasse.io/v1alpha1/namespaces/{namespace}/messagingusers";

    private static final Logger log = LoggerFactory.getLogger(HttpUserService.class.getName());

    private final UserApi userApi;

    public HttpUserService(UserApi userApi) {
        this.userApi = userApi;
    }

    private Response doRequest(String errorMessage, Callable<Response> request) throws Exception {
        try {
            return request.call();
        } catch (Exception e) {
            log.error(errorMessage, e);
            throw e;
        }
    }

    private static void verifyAuthorized(SecurityContext securityContext, String namespace, ResourceVerb verb) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "messagingusers", "user.enmasse.io"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUserList(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return doRequest("Error getting user list", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);
            UserList userList = new UserList();
            if (labelSelector != null) {
                Map<String, String> labels = AddressApiHelper.parseLabelSelector(labelSelector);
                userList.addAll(userApi.listUsersWithLabels(namespace, labels));
            } else {
                userList.addAll(userApi.listUsers(namespace));
            }
            return Response.ok(userList).build();
        });
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{userName}")
    public Response getUser(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("userName") String userNameWithAddressSpace) throws Exception {
        return doRequest("Error getting user " + userNameWithAddressSpace, () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.get);
            String addressSpace = parseAddressSpace(userNameWithAddressSpace);
            String realm = getRealmName(addressSpace, namespace);
            String userName = parseUserName(userNameWithAddressSpace);
            log.debug("Retrieving user {} in realm {} namespace {}", userName, realm, namespace);
            return userApi.getUserWithName(realm, userName)
                    .map(user -> Response.ok(user).build())
                    .orElseThrow(() -> new NotFoundException("User " + userNameWithAddressSpace + " not found"));
        });
    }

    private String getRealmName(String addressSpace, String namespace) {
        return namespace + "-" + addressSpace;
    }

    private static String parseAddressSpace(String userNameWithAddressSpace) {
        String [] parts = userNameWithAddressSpace.split("\\.");
        if (parts.length < 2) {
            throw new BadRequestException("User name '" + userNameWithAddressSpace + "' does not contain valid separator (.) to identify address space");
        }
        return parts[0];
    }

    private static String parseUserName(String userNameWithAddressSpace) {
        String [] parts = userNameWithAddressSpace.split("\\.");
        if (parts.length < 2) {
            throw new BadRequestException("User name '" + userNameWithAddressSpace + "' does not contain valid separator (.) to identify user");
        }
        return parts[1];
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createUser(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @PathParam("namespace") String namespace, @NotNull User input) throws Exception {
        return doRequest("Error creating user " + input.getMetadata().getName(), () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.create);

            String addressSpace = parseAddressSpace(input.getMetadata().getName());
            String realm = getRealmName(addressSpace, namespace);
            userApi.createUser(realm, input);
            User created = userApi.getUserWithName(realm, input.getSpec().getUsername()).orElse(input);
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(created.getMetadata().getName());
            return Response.created(builder.build()).entity(created).build();
        });
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{userName}")
    public Response replaceUser(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpaceName, @NotNull User payload) throws Exception {
        throw new UnsupportedOperationException("Replacing users are not supported");
    }

    @DELETE
    @Path("{userName}")
    public Response deleteUser(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("userName") String userNameWithAddressSpace) throws Exception {
        return doRequest("Error deleting user " + userNameWithAddressSpace, () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            String userName = parseUserName(userNameWithAddressSpace);
            String addressSpace = parseAddressSpace(userNameWithAddressSpace);
            String realm = getRealmName(addressSpace, namespace);
            log.debug("Deleting user {} in realm {} namespace {}", userName, realm, namespace);
            User user = userApi.getUserWithName(realm, userName)
                    .orElseThrow(() -> new NotFoundException("Unable to find user " + userNameWithAddressSpace));
            userApi.deleteUser(realm, user);
            return Response.ok().build();
        });
    }

    @DELETE
    public Response deleteUsers(@Context SecurityContext securityContext, @PathParam("namespace") String namespace) throws Exception {
        return doRequest("Error deleting address space s", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            userApi.deleteUsers(namespace);
            return Response.ok().build();
        });
    }

}
