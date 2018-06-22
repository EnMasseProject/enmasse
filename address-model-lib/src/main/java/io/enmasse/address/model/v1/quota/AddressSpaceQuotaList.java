/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.quota;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceQuotaList {
    @JsonProperty("apiVersion")
    private final String apiVersion = "enmasse.io/v1alpha1";

    @JsonProperty("kind")
    private final String kind = "AddressSpaceQuotaList";

    private final List<AddressSpaceQuota> addressSpaceQuotas;

    public List<AddressSpaceQuota> getItems() {
        return Collections.unmodifiableList(addressSpaceQuotas);
    }

    public AddressSpaceQuotaList(@JsonProperty("items") Collection<AddressSpaceQuota> addressSpaceQuotas) {
        this.addressSpaceQuotas = new ArrayList<>(addressSpaceQuotas);
    }
}
