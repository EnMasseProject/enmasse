/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.*;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.api.common.Status;
import io.enmasse.api.common.UuidGenerator;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
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

@Path(HttpAddressSpaceService.BASE_URI)
public class HttpAddressSpaceService {

    static final String BASE_URI = "/apis/enmasse.io/v1alpha1/namespaces/{namespace}/addressspaces";

    private static final Logger log = LoggerFactory.getLogger(HttpAddressSpaceService.class.getName());
    private final SchemaProvider schemaProvider;

    private final AddressSpaceApi addressSpaceApi;
    private final UuidGenerator uuidGenerator = new UuidGenerator();
    private final Clock clock;

    public HttpAddressSpaceService(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider, Clock clock) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
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
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "addressspaces", "enmasse.io"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddressSpaceList(@Context SecurityContext securityContext, @HeaderParam("Accept") String acceptHeader, @PathParam("namespace") String namespace, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return doRequest("Error getting address space list", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);
            Instant now = clock.instant();
            if (labelSelector != null) {
                Map<String, String> labels = AddressApiHelper.parseLabelSelector(labelSelector);
                return Response.ok(formatResponse(acceptHeader, now, removeSecrets(addressSpaceApi.listAddressSpacesWithLabels(namespace, labels)))).build();
            } else {
                return Response.ok(formatResponse(acceptHeader, now, removeSecrets(addressSpaceApi.listAddressSpaces(namespace)))).build();
            }
        });
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response getAddressSpace(@Context SecurityContext securityContext, @HeaderParam("Accept") String acceptHeader, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpaceName) throws Exception {
        return doRequest("Error getting address space " + addressSpaceName, () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.get);
            Instant now = clock.instant();
            return addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName)
                    .map(addressSpace -> Response.ok(formatResponse(acceptHeader, now, removeSecrets(addressSpace))).build())
                    .orElseGet(() -> Response.status(404).entity(Status.notFound("AddressSpace", addressSpaceName)).build());
        });
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createAddressSpace(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @PathParam("namespace") String namespace, @NotNull AddressSpace input) throws Exception {
        return doRequest("Error creating address space " + input.getName(), () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.create);
            AddressSpace addressSpace = setAddressSpaceDefaults(securityContext, namespace, input, null);
            addressSpace.validate();

            AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
            addressSpaceResolver.validate(addressSpace);
            addressSpaceApi.createAddressSpace(addressSpace);
            AddressSpace created = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpace.getName()).orElse(addressSpace);
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(created.getName());
            return Response.created(builder.build()).entity(removeSecrets(created)).build();
        });
    }

    private AddressSpace setAddressSpaceDefaults(SecurityContext securityContext, String namespace, AddressSpace addressSpace, AddressSpace existing) {
        if (existing == null) {
            if (addressSpace.getNamespace() == null) {
                addressSpace = new AddressSpace.Builder(addressSpace)
                        .setNamespace(namespace)
                        .build();
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

            if (addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID) == null) {
                addressSpace.putAnnotation(AnnotationKeys.INFRA_UUID, uuidGenerator.generateInfraUuid());
            }

            if (securityContext.isSecure() && securityContext.getUserPrincipal() != null) {
                String createdBy = RbacSecurityContext.getUserName(securityContext.getUserPrincipal());
                String createdByUid = RbacSecurityContext.getUserId(securityContext.getUserPrincipal());

                addressSpace.putAnnotation(AnnotationKeys.CREATED_BY, createdBy);
                addressSpace.putAnnotation(AnnotationKeys.CREATED_BY_UID, createdByUid);
            }
        } else {
            validateChanges(existing, addressSpace);
            Map<String, String> annotations = existing.getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
            }
            annotations.putAll(addressSpace.getAnnotations());

            Map<String, String> labels = existing.getLabels();
            if (labels == null) {
                labels = new HashMap<>();
            }
            labels.putAll(addressSpace.getLabels());

            addressSpace = new AddressSpace.Builder(existing)
                    .setEndpointList(addressSpace.getEndpoints())
                    .setNetworkPolicy(addressSpace.getNetworkPolicy())
                    .setAnnotations(annotations)
                    .setPlan(addressSpace.getPlan())
                    .setLabels(labels)
                    .build();
        }

        return addressSpace;
    }

    private void validateChanges(AddressSpace existing, AddressSpace addressSpace) {
        if (!existing.getType().equals(addressSpace.getType())) {
            throw new BadRequestException("Cannot change type of address space " + addressSpace.getName() + " from " + existing.getType() + " to " + addressSpace.getType());
        }

        if (!existing.getAuthenticationService().equals(addressSpace.getAuthenticationService())) {
            throw new BadRequestException("Cannot change authentication service of address space " + addressSpace.getName() + " from " + existing.getAuthenticationService() + " to " + addressSpace.getAuthenticationService());
        }

        validateAnnotation(existing, addressSpace, AnnotationKeys.REALM_NAME);
        validateAnnotation(existing, addressSpace, AnnotationKeys.INFRA_UUID);
        validateAnnotation(existing, addressSpace, AnnotationKeys.CREATED_BY);
        validateAnnotation(existing, addressSpace, AnnotationKeys.CREATED_BY_UID);
        validateLabel(existing, addressSpace, LabelKeys.ADDRESS_SPACE_TYPE);
        validateLabel(existing, addressSpace, LabelKeys.NAMESPACE);
    }

    private static void validateAnnotation(AddressSpace existing, AddressSpace addressSpace, String annotationKey) {
        if (existing.getAnnotation(annotationKey) == null) {
            return;
        }
        if (!existing.getAnnotation(annotationKey).equals(addressSpace.getAnnotation(annotationKey))) {
            throw new BadRequestException("Cannot change annotation " + annotationKey + " of existing address space " + addressSpace.getName());
        }
    }

    private static void validateLabel(AddressSpace existing, AddressSpace addressSpace, String labelKey) {
        if (existing.getLabel(labelKey) == null) {
            return;
        }
        if (!existing.getLabel(labelKey).equals(addressSpace.getLabel(labelKey))) {
            throw new BadRequestException("Cannot change label " + labelKey + " of existing address space " + addressSpace.getName());
        }
    }

    static AddressSpaceList removeSecrets(Collection<AddressSpace> addressSpaceList) {
        return addressSpaceList.stream()
                .map(HttpAddressSpaceService::removeSecrets)
                .collect(Collectors.toCollection(AddressSpaceList::new));
    }

    static AddressSpace removeSecrets(AddressSpace addressSpace) {
        return new AddressSpace.Builder(addressSpace)
                .setEndpointList(addressSpace.getEndpoints().stream()
                        .map(e -> {
                            if (e.getCertSpec().isPresent()) {
                                return new EndpointSpec.Builder(e)
                                        .setCertSpec(new CertSpec.Builder(e.getCertSpec().get())
                                                .setTlsKey(null)
                                                .build())
                                        .build();
                            } else {
                                return e;
                            }
                        }).collect(Collectors.toList()))
                .build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response replaceAddressSpace(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpaceName, @NotNull AddressSpace payload) throws Exception {
        checkRequestBodyNotNull(payload);
        checkAddressSpaceObjectNameNotNull(payload, addressSpaceName);
        checkMatchingAddressSpaceName(addressSpaceName, payload);
        return doRequest("Error replacing address space " + payload.getName(), () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.update);
            AddressSpace existing = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName).orElse(null);
            AddressSpace addressSpace = setAddressSpaceDefaults(securityContext, namespace, payload, existing);
            addressSpace.validate();

            AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
            addressSpaceResolver.validate(addressSpace);
            if (!addressSpaceApi.replaceAddressSpace(addressSpace)) {
                return Response.status(404).entity(Status.notFound("AddressSpace", addressSpaceName)).build();
            }
            AddressSpace replaced = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpace.getName()).orElse(addressSpace);
            return Response.ok().entity(removeSecrets(replaced)).build();
        });
    }

    private void checkRequestBodyNotNull(Object object) {
        if (object == null) {
            throw new BadRequestException("Missing request body");
        }
    }

    private void checkAddressSpaceObjectNameNotNull(AddressSpace addressSpace, String addressSpaceNameFromURL) {
        if (addressSpace.getName() == null) {
            throw new BadRequestException("the name of the object (" + addressSpaceNameFromURL + " based on URL) was undeterminable: name must be provided");
        }
    }

    private void checkMatchingAddressSpaceName(String addressSpaceNameFromURL, AddressSpace addressSpaceFromPayload) {
        if (addressSpaceFromPayload.getName() != null && !addressSpaceFromPayload.getName().equals(addressSpaceNameFromURL)) {
            throw new BadRequestException("the name of the object (" + addressSpaceFromPayload.getName() + ") does not match the name on the URL (" + addressSpaceNameFromURL + ")");
        }
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response deleteAddressSpace(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpaceName) throws Exception {
        return doRequest("Error deleting address space " + addressSpaceName, () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            AddressSpace addressSpace = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName).orElse(null);
            if (addressSpace == null) {
                return Response.status(404).entity(Status.notFound("AddressSpace", addressSpaceName)).build();
            }
            addressSpaceApi.deleteAddressSpace(addressSpace);
            return Response.ok(Status.successStatus(200, "AddressSpace", addressSpaceName, addressSpace.getUid())).build();
        });
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddressSpaces(@Context SecurityContext securityContext, @PathParam("namespace") String namespace) throws Exception {
        return doRequest("Error deleting address space s", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            addressSpaceApi.deleteAddressSpaces(namespace);
            return Response.ok(Status.successStatus(200)).build();
        });
    }

    private static final List<TableColumnDefinition> tableColumnDefinitions = Arrays.asList(
            new TableColumnDefinition("Name must be unique within a namespace.",
                    "name",
                    "Name",
                    0,
                    "string"),
            new TableColumnDefinition("Type of address space",
                    "",
                    "Type",
                    1,
                    "string"),
            new TableColumnDefinition("Plan of address space",
                    "",
                    "Plan",
                    1,
                    "string"),
            new TableColumnDefinition("The readiness of this address space.",
                    "",
                    "Ready",
                    0,
                    "string"),
            new TableColumnDefinition("The timestamp representing server time when this address space was created.",
                    "",
                    "Age",
                    0,
                    "string"),
            new TableColumnDefinition("The status messages reported for the address space.",
                    "",
                    "Status",
                    1,
                    "string"));

    static Object formatResponse(String headerParam, Instant now, AddressSpaceList addressSpaceList) {
        if (isTableFormat(headerParam)) {
            return new Table(new ListMeta(), tableColumnDefinitions, createRows(now, addressSpaceList));
        } else {
            return addressSpaceList;
        }
    }

    static Object formatResponse(String headerParam, Instant now, AddressSpace addressSpace) {
        if (isTableFormat(headerParam)) {
            return new Table(new ListMeta(), tableColumnDefinitions, createRows(now, Collections.singletonList(addressSpace)));
        } else {
            return addressSpace;
        }
    }

    private static boolean isTableFormat(String acceptHeader) {
        return acceptHeader != null && acceptHeader.contains("as=Table") && acceptHeader.contains("g=meta.k8s.io") && acceptHeader.contains("v=v1beta1");
    }

    private static List<TableRow> createRows(Instant now, List<AddressSpace> addressSpaceList) {
        return addressSpaceList.stream()
                .map(addressSpace -> new TableRow(
                        Arrays.asList(
                                addressSpace.getName(),
                                addressSpace.getType(),
                                addressSpace.getPlan(),
                                addressSpace.getStatus().isReady(),
                                Optional.ofNullable(addressSpace.getCreationTimestamp())
                                        .map(s -> TimeUtil.formatHumanReadable(Duration.between(TimeUtil.parseRfc3339(s), now)))
                                        .orElse(""),
                                String.join(". ", addressSpace.getStatus().getMessages())),
                        new PartialObjectMetadata(new ObjectMetaBuilder()
                                .withNamespace(addressSpace.getNamespace())
                                .withName(addressSpace.getName())
                                .withLabels(addressSpace.getLabels())
                                .withAnnotations(addressSpace.getAnnotations())
                                .withCreationTimestamp(addressSpace.getCreationTimestamp())
                                .withSelfLink(addressSpace.getSelfLink())
                                .withUid(addressSpace.getUid())
                                .withResourceVersion(addressSpace.getResourceVersion())
                                .build())))
                .collect(Collectors.toList());
    }
}
