/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.config.AnnotationKeys;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * An EnMasse Address addressspace.
 */
public class Address {
    private final String name;
    private final String namespace;
    private final String selfLink;
    private final String uid;
    private final String creationTimestamp;
    private final String resourceVersion;
    private final String address;
    private final String addressSpace;
    private final String type;
    private final String plan;
    private final Status status;
    private final Map<String, String> labels;
    private final Map<String, String> annotations;

    private Address(String name, String namespace, String selfLink, String uid, String creationTimestamp, String resourceVersion, String address, String addressSpace, String type, String plan, Status status, Map<String, String> labels, Map<String, String> annotations) {
        this.name = name;
        this.namespace = namespace;
        this.selfLink = selfLink;
        this.uid = uid;
        this.creationTimestamp = creationTimestamp;
        this.resourceVersion = resourceVersion;
        this.address = address;
        this.addressSpace = addressSpace;
        this.type = type;
        this.plan = plan;
        this.status = status;
        this.labels = labels;
        this.annotations = annotations;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getUid() {
        return uid;
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

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{address=").append(address).append(",");
        sb.append("name=").append(name).append(",");
        sb.append("uid=").append(uid).append(",");
        sb.append("annotations=").append(annotations).append(",");
        sb.append("type=").append(type).append(",");
        sb.append("plan=").append(plan).append(",");
        sb.append("status=").append(status).append(",");
        sb.append("resourceVersion=").append(resourceVersion).append("}");
        return sb.toString();
    }

    public void validate() {
        Objects.requireNonNull(name, "name not set");
        Objects.requireNonNull(namespace, "namespace not set");
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

        return address.equals(address1.address) &&
                addressSpace.equals(address1.addressSpace) &&
                namespace.equals(address1.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, addressSpace, namespace);
    }

    public String getLabel(String labelKey) {
        return labels.get(labelKey);
    }

    public void putLabel(String labelKey, String labelValue) {
        labels.put(labelKey, labelValue);
    }

    public static class Builder {
        private String name;
        private String namespace;
        private String uid;
        private String selfLink;
        private String creationTimestamp;
        private String resourceVersion;
        private String address;
        private String addressSpace;
        private String type;
        private String plan;
        private Status status = new Status(false);
        private Map<String, String> labels = new HashMap<>();
        private Map<String, String> annotations = new HashMap<>();

        public Builder() {
        }

        public Builder(io.enmasse.address.model.Address address) {
            this.name = address.getName();
            this.namespace = address.getNamespace();
            this.uid = address.getUid();
            this.address = address.getAddress();
            this.addressSpace = address.getAddressSpace();
            this.type = address.getType();
            this.plan = address.getPlan();
            this.status = new Status(address.getStatus());
            this.selfLink = address.getSelfLink();
            this.creationTimestamp = address.getCreationTimestamp();
            this.resourceVersion = address.getResourceVersion();
            this.labels = new HashMap<>(address.getLabels());
            this.annotations = new HashMap<>(address.getAnnotations());
        }

        public Builder setUid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder setName(String name) {
            this.name = KubeUtil.sanitizeName(name);
            return this;
        }

        public Builder setNamespace(String namespace) {
            this.namespace = namespace;
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

        public Builder setLabels(Map<String, String> labels) {
            this.labels = new HashMap<>(labels);
            return this;
        }

        public Builder putLabel(String key, String value) {
            this.labels.put(key, value);
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

        public Builder setSelfLink(String selfLink) {
            this.selfLink = selfLink;
            return this;
        }

        public Builder setCreationTimestamp(String creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public Builder setResourceVersion(String resourceVersion) {
            this.resourceVersion = resourceVersion;
            return this;
        }

        public Address build() {
            Objects.requireNonNull(address, "address not set");
            Objects.requireNonNull(type, "type not set");
            Objects.requireNonNull(status, "status not set");
            Objects.requireNonNull(labels, "labels not set");
            Objects.requireNonNull(annotations, "annotations not set");
            if (name == null) {
                String uuid = UUID.nameUUIDFromBytes(address.getBytes(StandardCharsets.UTF_8)).toString();
                putAnnotation(AnnotationKeys.UUID, uuid);
                name = KubeUtil.sanitizeWithUuid(address, uuid);
            }
            return new Address(name, namespace, selfLink, uid, creationTimestamp, resourceVersion, address, addressSpace, type, plan, status, labels, annotations);
        }
    }
}
