/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.common.Status;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.model.v1beta1.PartialObjectMetadata;
import io.enmasse.k8s.model.v1beta1.Table;
import io.enmasse.k8s.model.v1beta1.TableColumnDefinition;
import io.enmasse.k8s.model.v1beta1.TableRow;
import io.enmasse.k8s.util.TimeUtil;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserBuilder;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

@Path(HttpUserService.BASE_URI)
public class HttpUserService {

    static final String BASE_URI = "/apis/user.enmasse.io/{version:v1alpha1|v1beta1}/namespaces/{namespace}/messagingusers";

    private static final Logger log = LoggerFactory.getLogger(HttpUserService.class.getName());

    private final AddressSpaceApi addressSpaceApi;
    private final UserApi userApi;
    private final Clock clock;

    public HttpUserService(AddressSpaceApi addressSpaceApi, UserApi userApi, Clock clock) {
        this.addressSpaceApi = addressSpaceApi;
        this.userApi = userApi;
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

    private static void verifyAuthorized(SecurityContext securityContext, String namespace, ResourceVerb verb) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "messagingusers", "user.enmasse.io"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUserList(@Context SecurityContext securityContext, @HeaderParam("Accept") String acceptHeader, @PathParam("namespace") String namespace, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return doRequest("Error getting user list", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);

            Instant now = clock.instant();
            UserList userList = new UserList();
            if (labelSelector != null) {
                Map<String, String> labels = AddressApiHelper.parseLabelSelector(labelSelector);
                userList.getItems().addAll(userApi.listUsersWithLabels(namespace, labels).getItems());
            } else {
                userList.getItems().addAll(userApi.listUsers(namespace).getItems());
            }
            return Response.ok(formatResponse(acceptHeader, now, userList)).build();
        });
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{userName}")
    public Response getUser(@Context SecurityContext securityContext, @HeaderParam("Accept") String acceptHeader, @PathParam("namespace") String namespace, @PathParam("userName") String userNameWithAddressSpace) throws Exception {
        return doRequest("Error getting user " + userNameWithAddressSpace, () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.get);

            Instant now = clock.instant();
            String addressSpaceName = parseAddressSpace(userNameWithAddressSpace);
            checkAddressSpaceName(userNameWithAddressSpace, addressSpaceName);
            AddressSpace addressSpace = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName).orElse(null);
            if (addressSpace == null) {
                return Response.status(404).entity(Status.notFound("AddressSpace", addressSpaceName)).build();
            }
            String realm = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
            log.debug("Retrieving user {} in realm {} namespace {}", userNameWithAddressSpace, realm, namespace);

            return userApi.getUserWithName(realm, userNameWithAddressSpace)
                    .map(user -> Response.ok(formatResponse(acceptHeader, now, user)).build())
                    .orElseGet(() -> Response.status(404).entity(Status.notFound("MessagingUser", userNameWithAddressSpace)).build());
        });
    }

    private static String parseAddressSpace(String userNameWithAddressSpace) {
        String [] parts = userNameWithAddressSpace.split("\\.");
        if (parts.length < 2) {
            throw new BadRequestException("User name '" + userNameWithAddressSpace + "' does not contain valid separator (.) to identify address space");
        }
        return parts[0];
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createUser(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @PathParam("namespace") String namespace, @NotNull User input) throws Exception {
        return doRequest("Error creating user " + input.getMetadata().getName(), () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.create);
            User user = setUserDefaults(input, namespace);
            String addressSpaceName = parseAddressSpace(user.getMetadata().getName());
            checkAddressSpaceName(user.getMetadata().getName(), addressSpaceName);

            AddressSpace addressSpace = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName).orElse(null);
            if (addressSpace == null) {
                return Response.status(404).entity(Status.notFound("AddressSpace", addressSpaceName)).build();
            }
            String realm = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);

            userApi.createUser(realm, user);
            User created = userApi.getUserWithName(realm, user.getMetadata().getName()).orElse(user);
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(created.getMetadata().getName());
            return Response.created(builder.build()).entity(created).build();
        });
    }

    private User setUserDefaults(User user, String namespace) {
        if (user.getMetadata().getNamespace() == null) {
            user = new UserBuilder(user)
                    .withMetadata(new ObjectMetaBuilder(user.getMetadata())
                            .withNamespace(namespace)
                            .build())
                    .build();
        }
        return user;
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{userName}")
    public Response replaceUser(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("userName") String userNameWithAddressSpace, @NotNull User input) throws Exception {
        return doRequest("Error replacing user " + input.getMetadata().getName(), () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.update);
            User user = setUserDefaults(input, namespace);

            String addressSpaceName = parseAddressSpace(user.getMetadata().getName());
            checkAddressSpaceName(user.getMetadata().getName(), addressSpaceName);
            checkMatchingUserName(userNameWithAddressSpace, user);
            AddressSpace addressSpace = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName).orElse(null);
            if (addressSpace == null) {
                return Response.status(404).entity(Status.notFound("AddressSpace", addressSpaceName)).build();
            }
            String realm = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);

            if (!userApi.replaceUser(realm, user)) {
                return Response.status(404).entity(Status.notFound("MessagingUser", user.getMetadata().getName())).build();
            }
            User replaced = userApi.getUserWithName(realm, user.getMetadata().getName()).orElse(user);
            return Response.ok().entity(replaced).build();
        });
    }

    @DELETE
    @Path("{userName}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteUser(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("userName") String userNameWithAddressSpace) throws Exception {
        return doRequest("Error deleting user " + userNameWithAddressSpace, () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);

            String addressSpaceName = parseAddressSpace(userNameWithAddressSpace);
            checkAddressSpaceName(userNameWithAddressSpace, addressSpaceName);
            AddressSpace addressSpace = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName).orElse(null);
            if (addressSpace == null) {
                return Response.status(404).entity(Status.notFound("AddressSpace", addressSpaceName)).build();
            }
            String realm = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);

            log.debug("Deleting user {} in realm {} namespace {}", userNameWithAddressSpace, realm, namespace);
            User user = userApi.getUserWithName(realm, userNameWithAddressSpace).orElse(null);
            if (user == null) {
                return Response.status(404).entity(Status.notFound("MessagingUser", userNameWithAddressSpace)).build();
            }
            userApi.deleteUser(realm, user);
            return Response.ok(Status.successStatus(200)).build();
        });
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteUsers(@Context SecurityContext securityContext, @PathParam("namespace") String namespace) throws Exception {
        return doRequest("Error deleting address space s", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            userApi.deleteUsers(namespace);
            return Response.ok(Status.successStatus(200)).build();
        });
    }

    private static void checkAddressSpaceName(String pathName, String addressSpaceName) {
        if (addressSpaceName.isEmpty()) {
            throw new BadRequestException("The name of the object (" + pathName + ") is not valid");
        }
    }

    private static void checkMatchingUserName(String userNameFromUrl, User input) {
        if (input.getMetadata().getName() != null && !input.getMetadata().getName().equals(userNameFromUrl)) {
            throw new BadRequestException("The name of the object (" + input.getMetadata().getName() + ") does not match the name on the URL (" + userNameFromUrl + ")");
        }
    }

    private static final List<TableColumnDefinition> tableColumnDefinitions = Arrays.asList(
            new TableColumnDefinition("Name must be unique within a namespace.",
                    "name",
                    "Name",
                    0,
                    "string"),
            new TableColumnDefinition("User name used by clients.",
                    "",
                    "Username",
                    0,
                    "string"),
            new TableColumnDefinition("Authentication type.",
                    "",
                    "Type",
                    1,
                    "string"),
            new TableColumnDefinition("The timestamp representing server time when this user was created.",
                    "",
                    "Age",
                    0,
                    "string"));

    static Object formatResponse(String headerParam, Instant now, UserList userList) {
        if (isTableFormat(headerParam)) {
            return new Table(new ListMeta(), tableColumnDefinitions, createRows(userList, now));
        } else {
            return userList;
        }
    }

    static Object formatResponse(String headerParam, Instant now, User user) {
        if (isTableFormat(headerParam)) {
            UserList list = new UserList();
            list.getItems().add(user);
            return new Table(new ListMeta(), tableColumnDefinitions, createRows(list, now));
        } else {
            return user;
        }
    }

    static boolean isTableFormat(String acceptHeader) {
        return acceptHeader != null && acceptHeader.contains("as=Table") && acceptHeader.contains("g=meta.k8s.io") && acceptHeader.contains("v=v1beta1");
    }

    static List<TableRow> createRows(UserList userList, Instant now) {
        return userList.getItems().stream()
                .map(user -> new TableRow(
                        Arrays.asList(
                                user.getMetadata().getName(),
                                user.getSpec().getUsername(),
                                user.getSpec().getAuthentication().getType().name(),
                                Optional.ofNullable(user.getMetadata().getCreationTimestamp())
                                        .map(s -> TimeUtil.formatHumanReadable(Duration.between(TimeUtil.parseRfc3339(s), now)))
                                        .orElse("")),
                        new PartialObjectMetadata(new ObjectMetaBuilder()
                                .withNamespace(user.getMetadata().getNamespace())
                                .withName(user.getMetadata().getName())
                                .withCreationTimestamp(user.getMetadata().getCreationTimestamp())
                                .withSelfLink(user.getMetadata().getSelfLink())
                                .withResourceVersion(user.getMetadata().getResourceVersion())
                                .build())))
                .collect(Collectors.toList());
    }
}
