/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractWithAdditionalProperties.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
        )
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceSpecConnectorCredentials extends AbstractWithAdditionalProperties {
    @NotNull
    private StringOrSecretSelector username;

    @NotNull
    private StringOrSecretSelector password;

    public StringOrSecretSelector getUsername() {
        return username;
    }

    public void setUsername(StringOrSecretSelector username) {
        this.username = username;
    }

    public StringOrSecretSelector getPassword() {
        return password;
    }

    public void setPassword(StringOrSecretSelector password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "AddressSpaceSpecConnectorCredentials{" +
                "username=" + username +
                ", password=" + password +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSpecConnectorCredentials that = (AddressSpaceSpecConnectorCredentials) o;
        return Objects.equals(username, that.username) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
