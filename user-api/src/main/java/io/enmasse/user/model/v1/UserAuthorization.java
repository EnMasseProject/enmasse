/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAuthorization {
    private final List<String> addresses;
    private final List<Operation> operations;

    @JsonCreator
    public UserAuthorization(@JsonProperty("addresses") List<String> addresses,
                             @JsonProperty("operations") List<Operation> operations) {
        this.addresses = addresses;
        this.operations = operations;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public static class Builder {
        private List<String> addresses;
        private List<Operation> operations;

        public Builder setAddresses(List<String> addresses) {
            this.addresses = addresses;
            return this;
        }

        public Builder setOperations(List<Operation> operations) {
            this.operations = operations;
            return this;
        }

        public UserAuthorization build() {
            return new UserAuthorization(addresses, operations);
        }
    }
}
