/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.quota;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceQuota {
    @JsonProperty("apiVersion")
    private final String apiVersion = "enmasse.io/v1alpha1";

    @JsonProperty("kind")
    private final String kind = "AddressSpaceQuota";

    private final AddressSpaceQuotaMetadata metadata;
    private final AddressSpaceQuotaSpec spec;

    @JsonCreator
    public AddressSpaceQuota(@JsonProperty("metadata") AddressSpaceQuotaMetadata metadata,
                             @JsonProperty("spec") AddressSpaceQuotaSpec spec) {
        this.metadata = metadata;
        this.spec = spec;
    }

    public AddressSpaceQuotaMetadata getMetadata() {
        return metadata;
    }

    public AddressSpaceQuotaSpec getSpec() {
        return spec;
    }
}
