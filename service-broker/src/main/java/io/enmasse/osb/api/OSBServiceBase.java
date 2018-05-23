/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api;

import java.util.*;

import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.address.model.AddressSpace;
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
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "configmaps"))) {
            throw Exceptions.notAuthorizedException();
        }
    }


    protected Optional<AddressSpace> findAddressSpaceByInstanceId(String serviceInstanceId) {
        return addressSpaceApi.listAddressSpacesWithLabels(namespace, Collections.singletonMap(LabelKeys.SERVICE_INSTANCE_ID, serviceInstanceId)).stream().findAny();
    }

    protected Optional<AddressSpace> findAddressSpaceByName(String name) {
        return addressSpaceApi.listAddressSpaces(namespace).stream().filter(a -> a.getName().equals(name)).findAny();
    }

    protected AddressSpace createAddressSpace(String instanceId, String name, String type, String plan, String userId, String userName) throws Exception {
        AuthenticationService authService = new AuthenticationService.Builder()
                .setType(AuthenticationServiceType.STANDARD)
                .setDetails(Collections.emptyMap())
                .build();
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName(name)
                .setType(type)
                .setPlan(plan)
                .putAnnotation(AnnotationKeys.CREATED_BY, userName)
                .putAnnotation(AnnotationKeys.CREATED_BY_UID, userId)
                .setAuthenticationService(authService)
                .putLabel(LabelKeys.SERVICE_INSTANCE_ID, instanceId)
                .setEndpointList(null)
                .build();
        addressSpace = setDefaults(addressSpace, namespace);
        addressSpaceApi.createAddressSpace(addressSpace);
        log.info("Created MaaS addressspace {}", addressSpace.getName());
        return addressSpace;
    }

    private static AddressSpace setDefaults(AddressSpace addressSpace, String namespace) {
        if (addressSpace.getNamespace() == null) {
            addressSpace = new AddressSpace.Builder(addressSpace)
                    .setNamespace(namespace)
                    .build();
        }

        if (addressSpace.getAnnotation(AnnotationKeys.NAMESPACE) == null) {
            addressSpace.putAnnotation(AnnotationKeys.NAMESPACE, KubeUtil.sanitizeName(addressSpace.getNamespace() + "-" + addressSpace.getName()));
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

    protected boolean deleteAddressSpace(AddressSpace addressSpace) {
        log.info("Deleting address space : {}", addressSpace.getName());
        addressSpaceApi.deleteAddressSpace(addressSpace);
        return true;
    }
}
