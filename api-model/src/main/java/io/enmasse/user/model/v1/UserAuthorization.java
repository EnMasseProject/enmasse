/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
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


    @Override
    public String toString() {
        return "UserAuthorization{" +
                "addresses=" + addresses +
                ", operations=" + operations +
                '}';
    }

    public void validate() {
        Objects.requireNonNull(operations, "'operations' field must be set");
        if (operations.contains(Operation.send) || operations.contains(Operation.view) || operations.contains(Operation.recv)) {
            Objects.requireNonNull(addresses, "'addresses' field must be set for operations '" + operations + "'");
        }
    }
}
