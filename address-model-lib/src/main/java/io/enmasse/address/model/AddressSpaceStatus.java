/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the status of an address
 */
public class AddressSpaceStatus {
    private boolean isReady = false;
    private List<EndpointStatus> endpointStatuses = new ArrayList<>();
    private Set<String> messages = new HashSet<>();

    public AddressSpaceStatus(boolean isReady) {
        this.isReady = isReady;
    }

    public AddressSpaceStatus(AddressSpaceStatus other) {
        this.isReady = other.isReady();
        this.endpointStatuses = new ArrayList<>(other.getEndpointStatuses());
        this.messages.addAll(other.getMessages());
    }

    public boolean isReady() {
        return isReady;
    }

    public AddressSpaceStatus setReady(boolean isReady) {
        this.isReady = isReady;
        return this;
    }

    public Set<String> getMessages() {
        return messages;
    }

    public AddressSpaceStatus appendMessage(String message) {
        this.messages.add(message);
        return this;
    }

    public AddressSpaceStatus clearMessages() {
        this.messages.clear();
        return this;
    }

    public AddressSpaceStatus setMessages(Set<String> messages) {
        this.messages = messages;
        return this;
    }

    public List<EndpointStatus> getEndpointStatuses() {
        return Collections.unmodifiableList(endpointStatuses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceStatus status = (AddressSpaceStatus) o;
        return isReady == status.isReady &&
                Objects.equals(endpointStatuses, status.endpointStatuses) &&
                Objects.equals(messages, status.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isReady, endpointStatuses, messages);
    }


    @Override
    public String toString() {
        return new StringBuilder()
                .append("{isReady=").append(isReady)
                .append(",").append("endpointStatuses=").append(endpointStatuses)
                .append(",").append("messages=").append(messages)
                .append("}")
                .toString();
    }

    public AddressSpaceStatus setEndpointStatuses(List<EndpointStatus> endpointStatuses) {
        this.endpointStatuses = new ArrayList<>(endpointStatuses);
        return this;
    }

    public AddressSpaceStatus appendEndpointStatus(EndpointStatus endpointStatus) {
        endpointStatuses.add(endpointStatus);
        return this;
    }
}
