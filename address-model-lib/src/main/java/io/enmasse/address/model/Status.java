/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the status of an address
 */
public class Status {
    private boolean isReady = false;
    private Phase phase = Phase.Pending;
    private List<String> messages = new ArrayList<>();

    public Status(boolean isReady) {
        this.isReady = isReady;
    }

    public Status(io.enmasse.address.model.Status other) {
        this.isReady = other.isReady();
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

    public List<String> getMessages() {
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

    public Status setMessages(List<String> messages) {
        this.messages = messages;
        return this;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{isReady=").append(isReady)
                .append(",").append("phase=").append(phase)
                .append("}")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Status status = (Status) o;

        if (isReady != status.isReady) return false;
        return phase.equals(status.phase);
    }

    @Override
    public int hashCode() {
        int result = (isReady ? 1 : 0);
        result = 31 * result + phase.hashCode();
        return result;
    }

    public enum Phase {
        Pending,
        Configuring,
        Active,
        Failed,
        Terminating
    };
}
