/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the status of an address
 */
public class Status {
    private boolean isReady = false;
    private Phase phase = Phase.Pending;
    private Set<String> messages = new HashSet<>();

    public Status(boolean isReady) {
        this.isReady = isReady;
    }

    public Status(io.enmasse.address.model.Status other) {
        this.isReady = other.isReady();
        this.phase = other.getPhase();
        this.messages.addAll(other.getMessages());
    }

    public boolean isReady() {
        return isReady;
    }

    public Phase getPhase() {
        return phase;
    }

    public Status setReady(boolean isReady) {
        this.isReady = isReady;
        return this;
    }

    public Status setPhase(Phase phase) {
        this.phase = phase;
        return this;
    }

    public Set<String> getMessages() {
        return messages;
    }

    public Status appendMessage(String message) {
        this.messages.add(message);
        return this;
    }

    public Status clearMessages() {
        this.messages.clear();
        return this;
    }

    public Status setMessages(Set<String> messages) {
        this.messages = messages;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Status status = (Status) o;
        return isReady == status.isReady &&
                phase == status.phase &&
                Objects.equals(messages, status.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isReady, phase, messages);
    }


    @Override
    public String toString() {
        return new StringBuilder()
                .append("{isReady=").append(isReady)
                .append(",").append("phase=").append(phase)
                .append(",").append("messages=").append(messages)
                .append("}")
                .toString();
    }

    public enum Phase {
        Pending,
        Configuring,
        Active,
        Failed,
        Terminating
    };
}
