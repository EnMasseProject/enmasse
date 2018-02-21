/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * An EnMasse Address addressspace.
 */
public class Address {
    private final String name;
    private final String uuid;
    private final String address;
    private final String addressSpace;
    private final String type;
    private final String plan;
    private final Status status;
    private final String version;
    private final Map<String, String> annotations;

    private Address(String name, String uuid, String address, String addressSpace, String type, String plan, Status status, String version, Map<String, String> annotations) {
        this.name = name;
        this.uuid = uuid;
        this.address = address;
        this.addressSpace = addressSpace;
        this.type = type;
        this.plan = plan;
        this.status = status;
        this.version = version;
        this.annotations = annotations;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getAddressSpace() {
        return addressSpace;
    }

    public String getType() {
        return type;
    }

    public String getPlan() {
        return plan;
    }

    public Status getStatus() {
        return status;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{address=").append(address).append(",");
        sb.append("name=").append(name).append(",");
        sb.append("uuid=").append(uuid).append(",");
        sb.append("annotations=").append(annotations).append(",");
        sb.append("type=").append(type).append(",");
        sb.append("plan=").append(plan).append(",");
        sb.append("status=").append(status).append(",");
        sb.append("version=").append(version).append("}");
        return sb.toString();
    }

    public void validate() {
        Objects.requireNonNull(name, "name not set");
        Objects.requireNonNull(address, "address not set");
        Objects.requireNonNull(addressSpace, "addressSpace not set");
        Objects.requireNonNull(plan, "plan not set");
        Objects.requireNonNull(type, "type not set");
        Objects.requireNonNull(status, "status not set");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Address address1 = (Address) o;

        return address.equals(address1.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    public static class Builder {
        private String name;
        private String uuid;
        private String address;
        private String addressSpace;
        private String type;
        private String plan;
        private Status status = new Status(false);
        private String version;
        private Map<String, String> annotations = new HashMap<>();

        public Builder() {
        }

        public Builder(io.enmasse.address.model.Address address) {
            this.name = address.getName();
            this.uuid = address.getUuid();
            this.address = address.getAddress();
            this.addressSpace = address.getAddressSpace();
            this.type = address.getType();
            this.plan = address.getPlan();
            this.status = new Status(address.getStatus());
            this.version = address.getVersion();
            this.annotations = new HashMap<>(address.getAnnotations());
        }

        public Builder setUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder setName(String name) {
            this.name = KubeUtil.sanitizeName(name);
            return this;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder setAddressSpace(String addressSpace) {
            this.addressSpace = addressSpace;
            return this;
        }

        public Builder setAnnotations(Map<String, String> annotations) {
            this.annotations = new HashMap<>(annotations);
            return this;
        }

        public Builder putAnnotation(String key, String value) {
            this.annotations.put(key, value);
            return this;
        }

        public Builder setType(String addressType) {
            this.type = addressType;
            return this;
        }

        public Builder setPlan(String plan) {
            this.plan = plan;
            return this;
        }

        public Builder setStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Address build() {
            Objects.requireNonNull(address, "address not set");
            Objects.requireNonNull(type, "type not set");
            Objects.requireNonNull(status, "status not set");
            Objects.requireNonNull(annotations, "annotations not set");
            if (uuid == null) {
                uuid = UUID.nameUUIDFromBytes(address.getBytes(StandardCharsets.UTF_8)).toString();
            }
            if (name == null) {
                name = KubeUtil.sanitizeWithUuid(address, uuid);
            }
            return new Address(name, uuid, address, addressSpace, type, plan, status, version, annotations);
        }
    }
}
