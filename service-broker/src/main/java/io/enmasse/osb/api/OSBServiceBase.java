/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api;

import java.util.*;

import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceBuilder;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.api.common.UuidGenerator;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;

public abstract class OSBServiceBase {

    public static final String BASE_URI = "/osbapi/v2";

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    private final AddressSpaceApi addressSpaceApi;
    private final AuthApi authApi;
    private final SchemaProvider schemaProvider;
    private final String namespace;
    private final UuidGenerator uuidGenerator = new UuidGenerator();

    public OSBServiceBase(AddressSpaceApi addressSpaceApi, AuthApi authApi, SchemaProvider schemaProvider) {
        this.addressSpaceApi = addressSpaceApi;
        this.namespace = authApi.getNamespace();
        this.authApi = authApi;
        this.schemaProvider = schemaProvider;

    }

    protected ServiceMapping getServiceMapping() {
        return new ServiceMapping(schemaProvider.getSchema());
    }

    protected AuthApi getAuthApi() {
        return authApi;
    }

    protected void verifyAuthorized(SecurityContext securityContext, ResourceVerb verb) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "addressspaces", "enmasse.io"))) {
            throw Exceptions.notAuthorizedException();
        }
    }


    protected Optional<AddressSpace> findAddressSpaceByInstanceId(String serviceInstanceId) {
        return addressSpaceApi.listAllAddressSpacesWithLabels(Collections.singletonMap(LabelKeys.SERVICE_INSTANCE_ID, serviceInstanceId)).stream().findAny();
    }

    protected Optional<AddressSpace> findAddressSpace(String name, String namespace) {
        return addressSpaceApi.listAddressSpaces(namespace).stream().filter(a -> a.getMetadata().getName().equals(name)).findAny();
    }

    protected AddressSpace createAddressSpace(String instanceId, String name, String namespace, String type, String plan, String userId, String userName) throws Exception {
        AuthenticationService authService = new AuthenticationServiceBuilder()
                .withType(AuthenticationServiceType.STANDARD)
                .withDetails(Collections.emptyMap())
                .build();
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .addToAnnotations(AnnotationKeys.CREATED_BY, userName)
                .addToAnnotations(AnnotationKeys.CREATED_BY_UID, userId)
                .addToLabels(LabelKeys.SERVICE_INSTANCE_ID, instanceId)
                .endMetadata()

                .withNewSpec()
                .withType(type)
                .withPlan(plan)
                .withAuthenticationService(authService)
                .endSpec()

                .build();
        addressSpace = setDefaults(addressSpace, namespace);
        addressSpaceApi.createAddressSpace(addressSpace);
        log.info("Created MaaS addressspace {}", addressSpace.getMetadata().getName());
        return addressSpace;
    }

    private AddressSpace setDefaults(AddressSpace addressSpace, String namespace) {

        if (addressSpace.getMetadata().getNamespace() == null) {
            addressSpace = new AddressSpaceBuilder(addressSpace)
                    .editOrNewMetadata()
                    .withNamespace(namespace)
                    .endMetadata()
                    .build();
        }

        final Map<String, String> annotations = addressSpace.getMetadata().getAnnotations();
        final Map<String, String> labels = addressSpace.getMetadata().getLabels();

        annotations.putIfAbsent(AnnotationKeys.REALM_NAME, KubeUtil.sanitizeName(addressSpace.getMetadata().getNamespace() + "-" + addressSpace.getMetadata().getName()));
        annotations.putIfAbsent(AnnotationKeys.INFRA_UUID, uuidGenerator.generateInfraUuid());

        labels.putIfAbsent(LabelKeys.ADDRESS_SPACE_TYPE, addressSpace.getSpec().getType());
        labels.putIfAbsent(LabelKeys.NAMESPACE, addressSpace.getMetadata().getNamespace());

        return addressSpace;
    }

    protected boolean deleteAddressSpace(AddressSpace addressSpace) {
        log.info("Deleting address space : {}", addressSpace.getMetadata().getName());
        addressSpaceApi.deleteAddressSpace(addressSpace);
        return true;
    }
}
