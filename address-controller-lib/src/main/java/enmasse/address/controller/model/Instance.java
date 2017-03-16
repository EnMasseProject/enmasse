package enmasse.address.controller.model;

import java.util.Optional;

/**
 * Represents an instance of the EnMasse infrastructure
 */
public class Instance {
    private final InstanceId instanceId;
    private final Optional<String> messagingHost;
    private final Optional<String> mqttHost;
    private final Optional<String> consoleHost;

    public Instance(InstanceId instanceId, Optional<String> messagingHost, Optional<String> mqttHost, Optional<String> consoleHost) {
        this.instanceId = instanceId;
        this.messagingHost = messagingHost;
        this.mqttHost = mqttHost;
        this.consoleHost = consoleHost;
    }

    public Optional<String> messagingHost() {
        return messagingHost;
    }

    public Optional<String> mqttHost() {
        return mqttHost;
    }

    public Optional<String> consoleHost() {
        return consoleHost;
    }

    public InstanceId id() {
        return instanceId;
    }

    public static class Builder {
        private InstanceId instanceId;
        private Optional<String> messagingHost = Optional.empty();
        private Optional<String> mqttHost = Optional.empty();
        private Optional<String> consoleHost = Optional.empty();

        public Builder(InstanceId instanceId) {
            this.instanceId = instanceId;
        }

        public Builder instanceId(InstanceId instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder messagingHost(Optional<String> messagingHost) {
            this.messagingHost = messagingHost;
            return this;
        }

        public Builder mqttHost(Optional<String> mqttHost) {
            this.mqttHost = mqttHost;
            return this;
        }

        public Builder consoleHost(Optional<String> consoleHost) {
            this.consoleHost = consoleHost;
            return this;
        }

        public Instance build() {
            return new Instance(instanceId, messagingHost, mqttHost, consoleHost);
        }
    }
}
