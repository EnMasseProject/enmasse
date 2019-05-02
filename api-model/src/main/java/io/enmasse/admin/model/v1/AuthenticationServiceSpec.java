/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {
                @BuildableReference(AbstractWithAdditionalProperties.class)
        },
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"realm", "type", "none", "standard", "external"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationServiceSpec extends AbstractWithAdditionalProperties {

    private String realm;
    private AuthenticationServiceType type;
    private AuthenticationServiceSpecNone none;
    private AuthenticationServiceSpecStandard standard;
    private AuthenticationServiceSpecExternal external;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationServiceSpec that = (AuthenticationServiceSpec) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(realm, that.realm) &&
                Objects.equals(none, that.none) &&
                Objects.equals(standard, that.standard) &&
                Objects.equals(external, that.external);
    }

    @Override
    public int hashCode() {
        return Objects.hash(realm, type, none, standard, external);
    }

    @Override
    public String toString() {
        return "AuthenticationServiceSpec{" +
                "type='" + type + '\'' +
                ", realm='" + realm + '\'' +
                ", none=" + none +
                ", standard=" + standard +
                ", external=" + external +
                '}';
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public AuthenticationServiceSpecStandard getStandard() {
        return standard;
    }

    public void setStandard(AuthenticationServiceSpecStandard standard) {
        this.standard = standard;
    }

    public AuthenticationServiceSpecExternal getExternal() {
        return external;
    }

    public void setExternal(AuthenticationServiceSpecExternal external) {
        this.external = external;
    }

    public void setNone(AuthenticationServiceSpecNone none) {
        this.none = none;
    }

    public AuthenticationServiceSpecNone getNone() {
        return none;
    }

    public AuthenticationServiceType getType() {
        return type;
    }

    public void setType(AuthenticationServiceType type) {
        this.type = type;
    }
}
