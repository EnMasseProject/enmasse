/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

/**
 * Represents the status of an address
 */
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
public class AddressStatus extends AbstractWithAdditionalProperties {

    @JsonProperty("isReady")
    private boolean ready = false;
    private Phase phase = Phase.Pending;
    private List<String> messages = new ArrayList<>();
    private List<@Valid BrokerStatus> brokerStatuses = new ArrayList<>();
    private AddressPlanStatus planStatus;

    private List<@Valid AddressStatusForwarder> forwarderStatuses;

    public AddressStatus() {
    }

    public AddressStatus(boolean ready) {
        this.ready = ready;
    }

    public AddressStatus(AddressStatus other) {
        this.ready = other.isReady();
        this.phase = other.getPhase();
        this.messages = new ArrayList<>(other.getMessages());
        this.brokerStatuses = new ArrayList<>();
        for (BrokerStatus brokerStatus : other.getBrokerStatuses()) {
            brokerStatuses.add(new BrokerStatus(brokerStatus.getClusterId(), brokerStatus.getContainerId(), brokerStatus.getState()));
        }
        if (other.getForwarderStatuses() != null) {
            this.forwarderStatuses = new ArrayList<>();
            for (AddressStatusForwarder forwarderStatus : other.getForwarderStatuses()) {
                forwarderStatuses.add(new AddressStatusForwarderBuilder()
                        .withName(forwarderStatus.getName())
                        .withReady(forwarderStatus.isReady())
                        .withMessages(new ArrayList<>(forwarderStatus.getMessages()))
                        .build());
            }
        }

    }

    public boolean isReady() {
        return ready;
    }

    public Phase getPhase() {
        return phase;
    }

    public List<BrokerStatus> getBrokerStatuses() {
        return Collections.unmodifiableList(brokerStatuses);
    }

    public AddressStatus setReady(boolean ready) {
        this.ready = ready;
        return this;
    }

    public AddressStatus setPhase(Phase phase) {
        this.phase = phase;
        return this;
    }

    public List<String> getMessages() {
        return messages;
    }

    public AddressStatus appendMessage(String message) {
        this.messages.add(message);
        return this;
    }

    public AddressStatus clearMessages() {
        this.messages.clear();
        return this;
    }

    public AddressStatus setMessages(List<String> messages) {
        this.messages = new ArrayList<>(messages);
        return this;
    }

    public AddressStatus appendBrokerStatus(BrokerStatus brokerStatus) {
        this.brokerStatuses.add(brokerStatus);
        return this;
    }

    public AddressStatus setBrokerStatuses(List<BrokerStatus> brokerStatuses) {
        this.brokerStatuses = new ArrayList<>(brokerStatuses);
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressStatus status = (AddressStatus) o;
        return ready == status.ready &&
                phase == status.phase &&
                Objects.equals(messages, status.messages) &&
                Objects.equals(brokerStatuses, status.brokerStatuses) &&
                Objects.equals(planStatus, status.planStatus) &&
                Objects.equals(forwarderStatuses, status.forwarderStatuses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ready, phase, messages, brokerStatuses, planStatus, forwarderStatuses);
    }


    @Override
    public String toString() {
        return new StringBuilder()
                .append("{ready=").append(ready)
                .append(",").append("phase=").append(phase)
                .append(",").append("messages=").append(messages)
                .append(",").append("brokerStatuses=").append(brokerStatuses)
                .append(",").append("planStatus=").append(planStatus)
                .append(",").append("forwarderStatuses=").append(forwarderStatuses)
                .append("}")
                .toString();
    }

    public void addAllBrokerStatuses(List<BrokerStatus> toAdd) {
        brokerStatuses.addAll(toAdd);
    }

    public AddressPlanStatus getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(AddressPlanStatus planStatus) {
        this.planStatus = planStatus;
    }

    public List<AddressStatusForwarder> getForwarderStatuses() {
        return forwarderStatuses;
    }

    public void setForwarderStatuses(List<AddressStatusForwarder> forwarderStatuses) {
        this.forwarderStatuses = new ArrayList<>(forwarderStatuses);
    }
}
