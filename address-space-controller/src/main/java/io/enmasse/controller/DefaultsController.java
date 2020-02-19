/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;

public class DefaultsController implements Controller {
    private final AuthenticationServiceRegistry authenticationServiceRegistry;

    public DefaultsController(AuthenticationServiceRegistry authenticationServiceRegistry) {
        this.authenticationServiceRegistry = authenticationServiceRegistry;
    }

    @Override
    public AddressSpace reconcileActive(AddressSpace addressSpace) {
        AddressSpaceBuilder builder = new AddressSpaceBuilder(addressSpace);

        if (addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID) == null) {
            if (addressSpace.getStatus().getInfraUuid() != null) {
                builder.editOrNewMetadata()
                    .addToAnnotations(AnnotationKeys.INFRA_UUID, addressSpace.getStatus().getInfraUuid())
                    .endMetadata();
            } else {
                builder.editOrNewMetadata()
                        .addToAnnotations(AnnotationKeys.INFRA_UUID, KubeUtil.infraUuid(
                                addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName()))
                        .endMetadata();
            }
        }

        // Sets the infra uuid in the status section based on the annotation. This is required to avoid it being accidentally overwritten by users.
        if (addressSpace.getStatus().getInfraUuid() == null) {
            addressSpace.getStatus()
                    .setInfraUuid(builder.getMetadata().getAnnotations().get(AnnotationKeys.INFRA_UUID));
        }


        if (addressSpace.getAnnotation(AnnotationKeys.REALM_NAME) == null) {
            builder.editOrNewMetadata()
                    .addToAnnotations(AnnotationKeys.REALM_NAME, KubeUtil.getAddressSpaceRealmName(addressSpace))
                    .endMetadata();
        }

        if (addressSpace.getSpec().getAuthenticationService() == null || addressSpace.getSpec().getAuthenticationService().getName() == null) {
            authenticationServiceRegistry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService()).ifPresent(a ->
                    builder.editOrNewSpec().editOrNewAuthenticationService()
                            .withName(a.getMetadata().getName())
                            .endAuthenticationService()
                            .endSpec());
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "DefaultsController";
    }
}
