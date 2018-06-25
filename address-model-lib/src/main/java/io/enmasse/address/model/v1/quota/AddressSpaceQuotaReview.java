/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.quota;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceQuotaReview {
    @JsonProperty("apiVersion")
    private final String apiVersion = "enmasse.io/v1alpha1";

    @JsonProperty("kind")
    private final String kind = "AddressSpaceQuotaReview";

    private final AddressSpaceQuotaReviewSpec spec;
    private final AddressSpaceQuotaReviewStatus status;

    @JsonCreator
    public AddressSpaceQuotaReview(@JsonProperty("spec") AddressSpaceQuotaReviewSpec spec,
                                   @JsonProperty("status") AddressSpaceQuotaReviewStatus status) {
        this.spec = spec;
        this.status = status;
    }

    public AddressSpaceQuotaReviewSpec getSpec() {
        return spec;
    }

    public AddressSpaceQuotaReviewStatus getStatus() {
        return status;
    }
}
