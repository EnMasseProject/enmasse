/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.quota;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceQuotaSpec {
    private final String user;
    private final List<AddressSpaceQuotaRule> rules;

    @JsonCreator
    public AddressSpaceQuotaSpec(@JsonProperty("user") String user,
                                 @JsonProperty("rules") List<AddressSpaceQuotaRule> rules) {
        this.user = user;
        this.rules = rules;
    }

    public String getUser() {
        return user;
    }

    public List<AddressSpaceQuotaRule> getRules() {
        return Collections.unmodifiableList(rules);
    }
}
