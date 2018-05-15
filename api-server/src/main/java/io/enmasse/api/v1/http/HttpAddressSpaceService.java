/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.AddressSpaceResolver;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;
import java.util.concurrent.Callable;

@Path(HttpAddressSpaceService.BASE_URI)
public class HttpAddressSpaceService {

    static final String BASE_URI = "/apis/enmasse.io/v1alpha1/namespaces/{namespace}/addressspaces";

    private static final Logger log = LoggerFactory.getLogger(HttpAddressSpaceService.class.getName());
    private final SchemaProvider schemaProvider;

    private final AddressSpaceApi addressSpaceApi;
    public HttpAddressSpaceService(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
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
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "addressspaces"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddressSpaceList(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return doRequest("Error getting address space list", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);
            if (labelSelector != null) {
                Map<String, String> labels = AddressApiHelper.parseLabelSelector(labelSelector);
                return Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpacesWithLabels(namespace, labels))).build();
            } else {
                return Response.ok(new AddressSpaceList(addressSpaceApi.listAddressSpaces(namespace))).build();
            }
        });
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response getAddressSpace(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpaceName) throws Exception {
        return doRequest("Error getting address space " + addressSpaceName, () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.get);
            return addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName)
                    .map(addressSpace -> Response.ok(addressSpace).build())
                    .orElseThrow(() -> new NotFoundException("Address space " + addressSpaceName + " not found"));
        });
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createAddressSpace(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @PathParam("namespace") String namespace, @NotNull  AddressSpace input) throws Exception {
        return doRequest("Error creating address space " + input.getName(), () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.create);
            AddressSpace addressSpace = setAddressSpaceDefaults(input, namespace);

            AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
            addressSpaceResolver.validate(addressSpace);
            addressSpaceApi.createAddressSpace(addressSpace);
            AddressSpace created = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpace.getName()).orElse(addressSpace);
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(created.getName());
            return Response.created(builder.build()).entity(created).build();
        });
    }

    private AddressSpace setAddressSpaceDefaults(AddressSpace addressSpace, String namespace) {
        if (addressSpace.getNamespace() == null) {
            addressSpace = new AddressSpace.Builder(addressSpace)
                    .setNamespace(namespace)
                    .build();
        }

        if (addressSpace.getAnnotation(AnnotationKeys.NAMESPACE) == null) {
            addressSpace.putAnnotation(AnnotationKeys.NAMESPACE, KubeUtil.sanitizeName("enmasse-" + addressSpace.getNamespace() + "-" + addressSpace.getName()));
        }

        if (addressSpace.getAnnotation(AnnotationKeys.REALM_NAME) == null) {
            addressSpace.putAnnotation(AnnotationKeys.REALM_NAME, KubeUtil.sanitizeName(addressSpace.getNamespace() + "-" + addressSpace.getName()));
        }

        if (addressSpace.getLabel(LabelKeys.ADDRESS_SPACE_TYPE) == null) {
            addressSpace.putLabel(LabelKeys.ADDRESS_SPACE_TYPE, addressSpace.getType());
        }

        if (addressSpace.getLabel(LabelKeys.NAMESPACE) == null) {
            addressSpace.putLabel(LabelKeys.NAMESPACE, addressSpace.getNamespace());
        }

        return addressSpace;
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response replaceAddressSpace(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @NotNull  AddressSpace input) throws Exception {
        return doRequest("Error replacing address space " + input.getName(), () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.update);
            AddressSpace addressSpace = setAddressSpaceDefaults(input, namespace);

            AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
            addressSpaceResolver.validate(addressSpace);
            addressSpaceApi.replaceAddressSpace(addressSpace);
            AddressSpace replaced = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpace.getName()).orElse(addressSpace);
            return Response.ok().entity(replaced).build();
        });
    }

    @DELETE
    @Path("{addressSpace}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddressSpace(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpaceName) throws Exception {
        return doRequest("Error deleting address space " + addressSpaceName, () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            AddressSpace addressSpace = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName)
                    .orElseThrow(() -> new NotFoundException("Unable to find address space " + addressSpaceName));
            addressSpaceApi.deleteAddressSpace(addressSpace);
            return Response.ok().build();
        });
    }
}
