/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2;

import java.util.*;

import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.api.RbacSecurityContext;
import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;

public abstract class OSBServiceBase {

    public static final String BASE_URI = "/osbapi/v2";

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    private final AddressSpaceApi addressSpaceApi;
    private final ServiceMapping serviceMapping;
    private final Kubernetes kubernetes;

    public OSBServiceBase(AddressSpaceApi addressSpaceApi, Kubernetes kubernetes, ServiceMapping serviceMapping) {
        this.addressSpaceApi = addressSpaceApi;
        this.kubernetes = kubernetes;
        this.serviceMapping = serviceMapping;

    }

    protected ServiceMapping getServiceMapping() {
        return serviceMapping;
    }

    protected void verifyAuthorized(SecurityContext securityContext, ResourceVerb verb) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(kubernetes.getNamespace(), verb))) {
            throw OSBExceptions.notAuthorizedException();
        }
    }


    protected Optional<AddressSpace> findAddressSpaceByInstanceId(String serviceInstanceId) {
        return addressSpaceApi.listAddressSpacesWithLabels(Collections.singletonMap(LabelKeys.SERVICE_INSTANCE_ID, serviceInstanceId)).stream().findAny();
    }

    protected Optional<AddressSpace> findAddressSpaceByName(String name) {
        return addressSpaceApi.listAddressSpaces().stream().filter(a -> a.getName().equals(name)).findAny();
    }

    protected AddressSpace createAddressSpace(String instanceId, String name, String type, String plan) throws Exception {
        AuthenticationService authService = new AuthenticationService.Builder()
                .setType(AuthenticationServiceType.STANDARD)
                .setDetails(Collections.emptyMap())
                .build();
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName(name)
                .setType(type)
                .setPlan(plan)
                .setAuthenticationService(authService)
                .setEndpointList(null)
                .build();
        addressSpaceApi.createAddressSpaceWithLabels(addressSpace, Collections.singletonMap(LabelKeys.SERVICE_INSTANCE_ID, instanceId));
        log.info("Created MaaS addressspace {}", addressSpace.getName());
        return addressSpace;
    }

    protected boolean deleteAddressSpace(AddressSpace addressSpace) {
        log.info("Deleting address space : {}", addressSpace.getName());
        addressSpaceApi.deleteAddressSpace(addressSpace);
        return true;
    }

    protected Kubernetes getKubernetes() {
        return kubernetes;
    }
}
