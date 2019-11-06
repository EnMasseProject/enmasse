/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.enmasse.model.validation.AddressSpaceConnectorAddressRuleName;
import io.enmasse.model.validation.AddressSpaceConnectorAddressRulePattern;
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
@AddressSpaceConnectorAddressRuleName
@AddressSpaceConnectorAddressRulePattern
public class AddressSpaceSpecConnectorAddressRule {
    @NotNull
    private String name;
    @NotNull
    private String pattern;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSpecConnectorAddressRule that = (AddressSpaceSpecConnectorAddressRule) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pattern);
    }
}
