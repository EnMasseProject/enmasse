package enmasse.controller.model;

import java.util.Optional;

/**
 * Represents an instance of the EnMasse infrastructure
 */
public class Instance {
    private final InstanceId instanceId;
    private final Optional<String> messagingHost;
    private final Optional<String> mqttHost;
    private final Optional<String> consoleHost;
    private final Optional<String> uuid;
    private final Optional<String> certSecret;
    private final Status status;

    public Instance(InstanceId instanceId, Optional<String> messagingHost, Optional<String> mqttHost, Optional<String> consoleHost, Optional<String> uuid, Optional<String> certSecret, Status status) {
        this.instanceId = instanceId;
        this.messagingHost = messagingHost;
        this.mqttHost = mqttHost;
        this.consoleHost = consoleHost;
        this.uuid = uuid;
        this.certSecret = certSecret;
        this.status = status;
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

    public Optional<String> uuid() {
        return uuid;
    }

    public Optional<String> certSecret() {
        return certSecret;
    }

    public InstanceId id() {
        return instanceId;
    }

    public Status status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instance instance = (Instance) o;

        return instanceId.equals(instance.instanceId);
    }

    @Override
    public int hashCode() {
        return instanceId.hashCode();
    }

    public static class Builder {
        private InstanceId instanceId;
        private Optional<String> messagingHost = Optional.empty();
        private Optional<String> mqttHost = Optional.empty();
        private Optional<String> consoleHost = Optional.empty();
        private Optional<String> uuid = Optional.empty();
        private Optional<String> certSecret = Optional.empty();
        private Status status = new Status(false);

        public Builder(Instance instance) {
            this.instanceId = instance.id();
            this.messagingHost = instance.messagingHost;
            this.mqttHost = instance.mqttHost;
            this.consoleHost = instance.consoleHost;
            this.uuid = instance.uuid;
            this.certSecret = instance.certSecret;
            this.status = instance.status;
        }

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

        public Builder uuid(Optional<String> uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder certSecret(Optional<String> certSecret) {
            this.certSecret = certSecret;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Instance build() {
            return new Instance(instanceId, messagingHost, mqttHost, consoleHost, uuid, certSecret, status);
        }
    }

    public static class Status {
        private final boolean isReady;

        public Status(boolean isReady) {
            this.isReady = isReady;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Status status = (Status) o;

            return isReady == status.isReady;
        }

        @Override
        public int hashCode() {
            return (isReady ? 1 : 0);
        }

        public boolean isReady() {
            return isReady;
        }
    }
}
