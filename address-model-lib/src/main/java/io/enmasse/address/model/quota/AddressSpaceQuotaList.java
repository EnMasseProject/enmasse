/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.quota;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceQuotaList {
    @JsonProperty("apiVersion")
    private final String apiVersion = "enmasse.io/v1alpha1";

    @JsonProperty("kind")
    private final String kind = "AddressSpaceQuotaList";

    private final List<AddressSpaceQuota> addressSpaceQuotas;

    public AddressSpaceQuotaList(@JsonProperty("items") List<AddressSpaceQuota> addressSpaceQuotas) {
        this.addressSpaceQuotas = addressSpaceQuotas;
    }
}
