/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.v1.types.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;

@Path("/apis")
public class HttpApiRootService {
    private static final APIGroup userApiGroup =
            new APIGroup("user.enmasse.io", Arrays.asList(
                    new APIGroupVersion("user.enmasse.io/v1alpha1", "v1alpha1"),
                    new APIGroupVersion("user.enmasse.io/v1beta1", "v1beta1")),
                    new APIGroupVersion("user.enmasse.io/v1beta1", "v1beta1"),
                    null);

    private static final APIGroupList apiGroupList = new APIGroupList(Arrays.asList( userApiGroup));

    private static void verifyAuthorized(SecurityContext securityContext, String method, String path) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(path, method))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public APIGroupList getApiGroupList(@Context SecurityContext securityContext, @Context UriInfo uriInfo) {
        verifyAuthorized(securityContext, "get", uriInfo.getPath());
        return apiGroupList;
    }

    @GET
    @Path("user.enmasse.io")
    @Produces({MediaType.APPLICATION_JSON})
    public APIGroup getUserApiGroup(@Context SecurityContext securityContext, @Context UriInfo uriInfo) {
        verifyAuthorized(securityContext, "get", uriInfo.getPath());
        return userApiGroup;
    }


    private static final List<APIResource> enmasseUserResources = Arrays.asList(
                    new APIResource("messagingusers", "", true, "MessagingUser",
                                    Arrays.asList("create", "delete", "get", "list", "update")));

    private static final APIResourceList enmasseV1Alpha1UserResourceList = new APIResourceList("user.enmasse.io/v1alpha1", enmasseUserResources);

    @GET
    @Path("user.enmasse.io/v1alpha1")
    @Produces({MediaType.APPLICATION_JSON})
    public APIResourceList getUserApiGroupV1Alpha1(@Context SecurityContext securityContext, @Context UriInfo uriInfo) {
        // verifyAuthorized(securityContext, "get", uriInfo.getPath());
        return enmasseV1Alpha1UserResourceList;
    }

    private static final APIResourceList enmasseV1Beta1UserResourceList = new APIResourceList("user.enmasse.io/v1beta1", enmasseUserResources);

    @GET
    @Path("user.enmasse.io/v1beta1")
    @Produces({MediaType.APPLICATION_JSON})
    public APIResourceList getUserApiGroupV1Beta1(@Context SecurityContext securityContext, @Context UriInfo uriInfo) {
        // verifyAuthorized(securityContext, "get", uriInfo.getPath());
        return enmasseV1Beta1UserResourceList;
    }
}
