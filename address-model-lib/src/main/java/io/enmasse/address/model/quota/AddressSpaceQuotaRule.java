/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.quota;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceQuotaRule {
    private final int count;
    private final String type;
    private final String plan;

    @JsonCreator
    public AddressSpaceQuotaRule(@JsonProperty("count") int count,
                                 @JsonProperty("type") String type,
                                 @JsonProperty("plan") String plan) {
        this.count = count;
        this.type = type;
        this.plan = plan;
    }

    public String getType() {
        return type;
    }

    public String getPlan() {
        return plan;
    }

    public int getCount() {
        return count;
    }
}
