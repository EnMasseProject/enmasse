/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AddressSpaceStatusConnector extends AbstractWithAdditionalProperties {
    private String name;

    @JsonProperty("isReady")
    private boolean ready = false;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> messages = new ArrayList<>();

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public List<String> getMessages() {
        return messages;
    }


    public void appendMessage(String message) {
        this.messages.add(message);
    }

    public void clearMessages() {
        this.messages.clear();
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AddressSpaceStatusConnector{" +
                "name='" + name + '\'' +
                ", ready=" + ready +
                ", messages=" + messages +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceStatusConnector that = (AddressSpaceStatusConnector) o;
        return ready == that.ready &&
                Objects.equals(name, that.name) &&
                Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ready, messages);
    }
}
