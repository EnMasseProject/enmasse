/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import io.enmasse.address.model.*;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.CheckedFunction;
import io.enmasse.api.common.Exceptions;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
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
import io.enmasse.model.validation.DefaultValidator;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
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

    static final String BASE_URI = "/apis/enmasse.io/{version:v1alpha1|v1beta1}/namespaces/{namespace}/addressspaces";

    private static final Logger log = LoggerFactory.getLogger(HttpAddressSpaceService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final SchemaProvider schemaProvider;

    private final AddressSpaceApi addressSpaceApi;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private final UuidGenerator uuidGenerator = new UuidGenerator();
    private final Clock clock;

    public HttpAddressSpaceService(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider, Clock clock, AuthenticationServiceRegistry authenticationServiceRegistry) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
        this.clock = clock;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
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
    public Response createAddressSpace(@Context SecurityContext securityContext, @Context UriInfo uriInfo, @PathParam("namespace") String namespace, @NotNull @Valid AddressSpace input) throws Exception {
        return doRequest("Error creating address space " + input.getMetadata().getName(), () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.create);
            AddressSpace addressSpace = setAddressSpaceDefaults(securityContext, namespace, input, null);
            DefaultValidator.validate(addressSpace);

            AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
            addressSpaceResolver.validate(addressSpace);
            addressSpaceApi.createAddressSpace(addressSpace);
            AddressSpace created = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpace.getMetadata().getName()).orElse(addressSpace);
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(created.getMetadata().getName());
            return Response.created(builder.build()).entity(removeSecrets(created)).build();
        });
    }

    @PATCH
    @Consumes({MediaType.APPLICATION_JSON_PATCH_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response patchAddressSpace(@Context SecurityContext securityContext,
                                      @PathParam("namespace") String namespace,
                                      @PathParam("addressSpace") String addressSpaceName,
                                      @NotNull JsonPatch patch) throws Exception {

        return doPatch(securityContext, namespace, addressSpaceName, patch::apply);
    }

    @PATCH
    @Consumes({"application/merge-patch+json", "application/strategic-merge-patch+json"})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response patchAddressSpace(@Context SecurityContext securityContext,
                                      @PathParam("namespace") String namespace,
                                      @PathParam("addressSpace") String addressSpaceName,
                                      @NotNull JsonMergePatch patch) throws Exception {


        return doPatch(securityContext, namespace, addressSpaceName, patch::apply);
    }

    private Response doPatch(@Context SecurityContext securityContext, String namespace, String addressSpaceName,
                             CheckedFunction<JsonNode, JsonNode, JsonPatchException> patcher) throws Exception {
        return doRequest("Error patching address space " + addressSpaceName, () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.patch);

            Optional<AddressSpace> existing = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName);

            if (!existing.isPresent()) {
                return Response.status(404).entity(Status.notFound("AddressSpace", addressSpaceName)).build();
            }

            JsonNode source = MAPPER.valueToTree(existing.get());

            JsonNode patched = patcher.apply(source);

            AddressSpace replacement = MAPPER.treeToValue(patched, AddressSpace.class);

            replacement = setAddressSpaceDefaults(securityContext, namespace, replacement, existing.get());
            DefaultValidator.validate(replacement);

            AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
            addressSpaceResolver.validate(replacement);
            if (!addressSpaceApi.replaceAddressSpace(replacement)) {
                return Response.status(400).entity(Status.failureStatus(400,"AddressSpace", addressSpaceName)).build();
            }
            AddressSpace replaced = addressSpaceApi.getAddressSpaceWithName(namespace, replacement.getMetadata().getName()).orElse(replacement);
            return Response.ok().entity(removeSecrets(replaced)).build();
        });
    }


    private AddressSpace setAddressSpaceDefaults(SecurityContext securityContext, String namespace, AddressSpace addressSpace, AddressSpace existing) {
        if (existing == null) {
            if (addressSpace.getMetadata().getNamespace() == null) {
                addressSpace.getMetadata().setNamespace(namespace);
            }

            addressSpace.putAnnotationIfAbsent(AnnotationKeys.REALM_NAME, KubeUtil.getAddressSpaceRealmName(addressSpace));
            addressSpace.putLabelIfAbsent(LabelKeys.ADDRESS_SPACE_TYPE, addressSpace.getSpec().getType());
            addressSpace.putLabelIfAbsent(LabelKeys.NAMESPACE, addressSpace.getMetadata().getNamespace());
            addressSpace.putAnnotationIfAbsent(AnnotationKeys.INFRA_UUID, uuidGenerator.generateInfraUuid());

            if (securityContext.isSecure() && securityContext.getUserPrincipal() != null) {
                String createdBy = RbacSecurityContext.getUserName(securityContext.getUserPrincipal());
                String createdByUid = RbacSecurityContext.getUserId(securityContext.getUserPrincipal());

                addressSpace.putAnnotation(AnnotationKeys.CREATED_BY, createdBy);
                addressSpace.putAnnotation(AnnotationKeys.CREATED_BY_UID, createdByUid);
            }

            if (addressSpace.getSpec().getAuthenticationService() == null) {
                addressSpace.getSpec().setAuthenticationService(resolveDefaultAuthService());
            } else {
                validateAuthenticationService(addressSpace.getSpec().getAuthenticationService());
            }

            addressSpace.getStatus().setPhase(Phase.Pending);
        } else {
            validateChanges(existing, addressSpace);

            Map<String, String> annotations = existing.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
            }
            annotations.putAll(addressSpace.getMetadata().getAnnotations());

            Map<String, String> labels = existing.getMetadata().getLabels();
            if (labels == null) {
                labels = new HashMap<>();
            }
            labels.putAll(addressSpace.getMetadata().getLabels());

            addressSpace = new AddressSpaceBuilder(existing)

                    .editOrNewMetadata()
                    .withAnnotations(annotations)
                    .withLabels(labels)
                    .endMetadata()

                    .editOrNewSpec()
                    .withEndpoints(addressSpace.getSpec().getEndpoints())
                    .withNetworkPolicy(addressSpace.getSpec().getNetworkPolicy())
                    .withPlan(addressSpace.getSpec().getPlan())
                    .withConnectors(addressSpace.getSpec().getConnectors())
                    .endSpec()

                    .build();
            addressSpace.getStatus().setPhase(Phase.Pending);
        }

        return addressSpace;
    }

    private void validateAuthenticationService(AuthenticationService authenticationService) {
        String name = authenticationService.getName();
        if (name == null) {
            name = authenticationService.getType().getName();
        }
        if (authenticationServiceRegistry.findAuthenticationServiceByName(name).isEmpty()) {
            throw new BadRequestException("Unable to find authentication service '" + name + "'");
        }
    }

    private AuthenticationService resolveDefaultAuthService() {
        io.enmasse.admin.model.v1.AuthenticationService defaultAuthService = authenticationServiceRegistry.resolveDefaultAuthenticationService().orElseThrow(
                () -> new InternalServerErrorException("No authentication service specified, and unable to resolve default: no authentication services found"));

        AuthenticationService authenticationService = new AuthenticationService();
        authenticationService.setName(defaultAuthService.getMetadata().getName());
        return authenticationService;
    }

    private void validateChanges(AddressSpace existing, AddressSpace addressSpace) {
        if (!existing.getSpec().getType().equals(addressSpace.getSpec().getType())) {
            throw new BadRequestException("Cannot change type of address space " + addressSpace.getMetadata().getName() + " from " + existing.getSpec().getType() + " to " + addressSpace.getSpec().getType());
        }

        if (addressSpace.getSpec().getAuthenticationService() != null && !existing.getSpec().getAuthenticationService().equals(addressSpace.getSpec().getAuthenticationService())) {
            throw new BadRequestException("Cannot change authentication service of address space " + addressSpace.getMetadata().getName() + " from " + existing.getSpec().getAuthenticationService() + " to " + addressSpace.getSpec().getAuthenticationService());
        }

        overrideAnnotation(existing, addressSpace, AnnotationKeys.REALM_NAME);
        overrideAnnotation(existing, addressSpace, AnnotationKeys.APPLIED_PLAN);
        overrideAnnotation(existing, addressSpace, AnnotationKeys.APPLIED_INFRA_CONFIG);
        overrideAnnotation(existing, addressSpace, AnnotationKeys.INFRA_UUID);
        overrideAnnotation(existing, addressSpace, AnnotationKeys.CREATED_BY);
        overrideAnnotation(existing, addressSpace, AnnotationKeys.CREATED_BY_UID);
        overrideLabel(existing, addressSpace, LabelKeys.ADDRESS_SPACE_TYPE);
        overrideLabel(existing, addressSpace, LabelKeys.NAMESPACE);
    }

    private static void overrideAnnotation(AddressSpace existing, AddressSpace addressSpace, String annotationKey) {
        if (existing.getAnnotation(annotationKey) == null) {
            return;
        }
        if (!existing.getAnnotation(annotationKey).equals(addressSpace.getAnnotation(annotationKey))) {
            addressSpace.putAnnotation(annotationKey, existing.getAnnotation(annotationKey));
        }
    }

    private static void overrideLabel(AddressSpace existing, AddressSpace addressSpace, String labelKey) {
        if (existing.getLabel(labelKey) == null) {
            return;
        }
        if (!existing.getLabel(labelKey).equals(addressSpace.getLabel(labelKey))) {
            addressSpace.putLabel(labelKey, existing.getLabel(labelKey));
        }
    }

    static AddressSpaceList removeSecrets(Collection<AddressSpace> addressSpaceList) {

        final AddressSpaceList result = new AddressSpaceList();

        addressSpaceList.stream()
                .map(HttpAddressSpaceService::removeSecrets)
                .forEachOrdered(result.getItems()::add);

        return result;
    }

    static AddressSpace removeSecrets(AddressSpace addressSpace) {
        return new AddressSpaceBuilder(addressSpace)
                .editOrNewSpec()
                .withEndpoints(
                        addressSpace.getSpec().getEndpoints().stream()
                                .map(HttpAddressSpaceService::removeSecret)
                                .collect(Collectors.toList()))
                .endSpec()
                .build();
    }

    private static EndpointSpec removeSecret (final EndpointSpec e) {
        if (e.getCert() != null ) {
            return new EndpointSpecBuilder(e)
                    .withCert(new CertSpecBuilder(e.getCert())
                            .withTlsKey(null)
                            .build())
                    .build();
        } else {
            return e;
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response replaceAddressSpace(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("addressSpace") String addressSpaceName, @NotNull @Valid AddressSpace payload) throws Exception {
        checkRequestBodyNotNull(payload);
        checkAddressSpaceObjectNameNotNull(payload, addressSpaceName);
        checkMatchingAddressSpaceName(addressSpaceName, payload);
        return doRequest("Error replacing address space " + payload.getMetadata().getName(), () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.update);
            AddressSpace existing = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceName).orElse(null);
            AddressSpace addressSpace = setAddressSpaceDefaults(securityContext, namespace, payload, existing);
            DefaultValidator.validate(addressSpace);

            AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
            addressSpaceResolver.validate(addressSpace);
            if (!addressSpaceApi.replaceAddressSpace(addressSpace)) {
                return Response.status(404).entity(Status.notFound("AddressSpace", addressSpaceName)).build();
            }
            AddressSpace replaced = addressSpaceApi.getAddressSpaceWithName(namespace, addressSpace.getMetadata().getName()).orElse(addressSpace);
            return Response.ok().entity(removeSecrets(replaced)).build();
        });
    }

    private void checkRequestBodyNotNull(Object object) {
        if (object == null) {
            throw new BadRequestException("Missing request body");
        }
    }

    private void checkAddressSpaceObjectNameNotNull(AddressSpace addressSpace, String addressSpaceNameFromURL) {
        if (addressSpace.getMetadata().getName() == null) {
            throw new BadRequestException("the name of the object (" + addressSpaceNameFromURL + " based on URL) was undeterminable: name must be provided");
        }
    }

    private void checkMatchingAddressSpaceName(String addressSpaceNameFromURL, AddressSpace addressSpaceFromPayload) {
        if (addressSpaceFromPayload.getMetadata().getName() != null && !addressSpaceFromPayload.getMetadata().getName().equals(addressSpaceNameFromURL)) {
            throw new BadRequestException("the name of the object (" + addressSpaceFromPayload.getMetadata().getName() + ") does not match the name on the URL (" + addressSpaceNameFromURL + ")");
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
            return Response.ok(Status.successStatus(200, "AddressSpace", addressSpaceName, addressSpace.getMetadata().getUid())).build();
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
            new TableColumnDefinition("The phase of this address space.",
                    "",
                    "Phase",
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
            return new Table(new ListMeta(), tableColumnDefinitions, createRows(now, addressSpaceList.getItems()));
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
                                addressSpace.getMetadata().getName(),
                                addressSpace.getSpec().getType(),
                                addressSpace.getSpec().getPlan(),
                                addressSpace.getStatus().isReady(),
                                addressSpace.getStatus().getPhase(),
                                Optional.ofNullable(addressSpace.getMetadata().getCreationTimestamp())
                                        .map(s -> TimeUtil.formatHumanReadable(Duration.between(TimeUtil.parseRfc3339(s), now)))
                                        .orElse(""),
                                String.join(". ", addressSpace.getStatus().getMessages())),
                        new PartialObjectMetadata(new ObjectMetaBuilder()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(addressSpace.getMetadata().getName())
                                .withLabels(addressSpace.getMetadata().getLabels())
                                .withAnnotations(addressSpace.getMetadata().getAnnotations())
                                .withCreationTimestamp(addressSpace.getMetadata().getCreationTimestamp())
                                .withSelfLink(addressSpace.getMetadata().getSelfLink())
                                .withUid(addressSpace.getMetadata().getUid())
                                .withResourceVersion(addressSpace.getMetadata().getResourceVersion())
                                .build())))
                .collect(Collectors.toList());
    }
}
