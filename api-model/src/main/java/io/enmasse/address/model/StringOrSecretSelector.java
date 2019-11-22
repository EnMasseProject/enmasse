/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

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
public class StringOrSecretSelector extends AbstractWithAdditionalProperties {
    private String value;
    private SecretKeySelector valueFromSecret;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SecretKeySelector getValueFromSecret() {
        return valueFromSecret;
    }

    public void setValueFromSecret(SecretKeySelector valueFromSecret) {
        this.valueFromSecret = valueFromSecret;
    }

    @Override
    public String toString() {
        return "StringOrSecretSelector{" +
                ", valueFromSecret=" + valueFromSecret +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringOrSecretSelector that = (StringOrSecretSelector) o;
        return Objects.equals(value, that.value) &&
                Objects.equals(valueFromSecret, that.valueFromSecret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, valueFromSecret);
    }
}
