/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.enmasse.admin.model.AddressSpacePlan;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.common.model.CustomResourceWithAdditionalProperties;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.sundr.builder.annotations.Buildable;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.BuildableReference;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(ObjectMeta.class)}

)
@DefaultCustomResource
@SuppressWarnings("serial")
@Version(CoreCrd.VERSION)
@Group(CoreCrd.GROUP)
public class AddressSpaceSchema extends CustomResourceWithAdditionalProperties<AddressSpaceSchemaSpec,  AddressSpaceSchemaStatus> {

    public static final String KIND = "AddressSpaceSchema";

    // for builders - probably will be fixed by https://github.com/fabric8io/kubernetes-client/pull/1346
    private ObjectMeta metadata;
    @NotNull @Valid
    private AddressSpaceSchemaSpec spec;

    @Override
    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }


    public void setSpec(final AddressSpaceSchemaSpec spec) {
        this.spec = spec;
    }

    public AddressSpaceSchemaSpec getSpec() {
        return this.spec;
    }

    public static AddressSpaceSchema fromAddressSpaceType(final AddressSpaceType addressSpaceType, final List<AuthenticationService> authenticationServiceList) {
        if (addressSpaceType == null) {
            return null;
        }

        return new AddressSpaceSchemaBuilder()
                .withNewMetadata()
                .withName(addressSpaceType.getName())
                .endMetadata()

                .editOrNewSpec()
                .withDescription(addressSpaceType.getDescription())
                .withAddressTypes(addressSpaceType.getAddressTypes().stream()
                        .map(AddressTypeInformation::fromAddressType)
                        .collect(Collectors.toList()))
                .withPlans(addressSpaceType.getPlans().stream()
                        .sorted(Comparator.comparingInt(AddressSpacePlan::getDisplayOrder))
                        .map(plan -> new AddressSpacePlanDescription(plan.getMetadata().getName(), plan.getDisplayName(), plan.getShortDescription(), plan.getResourceLimits()))
                        .collect(Collectors.toList()))
                .withAuthenticationServices(authenticationServiceList.stream()
                        .map(authenticationService -> authenticationService.getMetadata().getName())
                        .collect(Collectors.toList()))
                .withRouteServicePorts(addressSpaceType.getRouteServicePorts())
                .withCertificateProviderTypes(addressSpaceType.getCertificateProviderTypes())
                .withEndpointExposeTypes(addressSpaceType.getEndpointExposeTypes())
                .endSpec()
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSchema that = (AddressSpaceSchema) o;
        return Objects.equals(spec, that.spec) &&
                Objects.equals(getMetadata(), that.getMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec, getMetadata());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("metadata=").append(getMetadata());
        sb.append(",");
        sb.append("spec=").append(this.spec);
        return sb.append("}").toString();
    }
}
