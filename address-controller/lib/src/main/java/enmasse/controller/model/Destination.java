/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.controller.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single destination in the addressing config. It is identified by an address and
 * additional properties that determine the semantics.
 */
public final class Destination {
    private final String address;
    private final String group;
    private final boolean storeAndForward;
    private final boolean multicast;
    private final Optional<String> flavor;
    private final Optional<String> uuid;
    private final Status status;

    public Destination(String address, String group, boolean storeAndForward, boolean multicast, Optional<String> flavor, Optional<String> uuid, Status status) {
        Objects.requireNonNull(flavor);
        Objects.requireNonNull(uuid);
        this.group = group;
        this.address = address;
        this.storeAndForward = storeAndForward;
        this.multicast = multicast;
        this.flavor = flavor;
        this.uuid = uuid;
        this.status = status;
    }

    public String address() {
        return address;
    }

    public String group() { return group; }

    public boolean storeAndForward() {
        return storeAndForward;
    }

    public boolean multicast() {
        return multicast;
    }

    public Optional<String> flavor() {
        return flavor;
    }

    public Optional<String> uuid() {
        return uuid;
    }

    public Status status() {
        return status;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{address=").append(address).append(",")
                .append("group=").append(group).append(",")
                .append("storeAndForward=").append(storeAndForward).append(",")
                .append("multicast=").append(multicast).append(",")
                .append("flavor=").append(flavor).append(",")
                .append("uuid=").append(uuid).append("}");
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Destination that = (Destination) o;

        if (!address.equals(that.address)) return false;
        if (!uuid.equals(that.uuid)) return false;
        if (!group.equals(that.group)) return false;
        if (storeAndForward != that.storeAndForward) return false;
        if (multicast != that.multicast) return false;
        return flavor.equals(that.flavor);
    }

    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + group.hashCode();
        result = 31 * result + (storeAndForward ? 1 : 0);
        result = 31 * result + (multicast ? 1 : 0);
        result = 31 * result + flavor.hashCode();
        result = 31 * result + uuid.hashCode();
        return result;
    }

    public static class Builder {
        private String address;
        private String group;
        private boolean storeAndForward = false;
        private boolean multicast = false;
        private Optional<String> flavor = Optional.empty();
        private Optional<String> uuid = Optional.empty();
        private Status status = new Status(false);

        public Builder(String address, String group) {
            this.address = address;
            this.group = group;
        }

        public Builder(Destination destination) {
            this.address = destination.address();
            this.group = destination.group();
            this.storeAndForward = destination.storeAndForward();
            this.multicast = destination.multicast();
            this.flavor = destination.flavor();
            this.uuid = destination.uuid();
            this.status = destination.status();
        }

        public Builder() {}

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder storeAndForward(boolean storeAndForward) {
            this.storeAndForward = storeAndForward;
            return this;
        }

        public Builder multicast(boolean multicast) {
            this.multicast = multicast;
            return this;
        }

        public Builder flavor(Optional<String> flavor) {
            this.flavor = flavor;
            return this;
        }

        public Builder uuid(Optional<String> uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Optional<String> flavor() {
            return flavor;
        }

        public Destination build() {
            return new Destination(address, group, storeAndForward, multicast, flavor, uuid, status);
        }

        public String group() {
            return group;
        }

        public boolean storeAndForward() {
            return storeAndForward;
        }

        public boolean multicast() {
            return multicast;
        }

        public String address() {
            return address;
        }
    }

    public static class Status {
        private final boolean isReady;
        private final Optional<String> message;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Status status = (Status) o;

            if (isReady != status.isReady) return false;
            return message.equals(status.message);
        }

        @Override
        public int hashCode() {
            int result = (isReady ? 1 : 0);
            result = 31 * result + message.hashCode();
            return result;
        }

        public Status(boolean isReady) {
            this(isReady, null);
        }

        public Status(boolean isReady, String message) {
            this.isReady = isReady;
            this.message = Optional.ofNullable(message);
        }

        public boolean isReady() {
            return isReady;
        }

        public Optional<String> getMessage() {
            return message;
        }
    }
}
