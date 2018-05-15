/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.config.AnnotationKeys;

import java.util.*;

/**
 * An EnMasse AddressSpace.
 */
public class AddressSpace {
    private final String name;
    private final String namespace;
    private final String typeName;
    private final String selfLink;
    private final String uid;
    private final String creationTimestamp;
    private final String resourceVersion;
    private final Map<String, String> labels;
    private final Map<String, String> annotations;

    private final List<Endpoint> endpointList;
    private final String planName;
    private final AuthenticationService authenticationService;
    private final Status status;

    private AddressSpace(String name, String namespace, String typeName, String selfLink, String creationTimestamp, String resourceVersion, List<Endpoint> endpointList, String planName, AuthenticationService authenticationService, Status status, String uid, Map<String, String> labels, Map<String, String> annotations) {
        this.name = name;
        this.namespace = namespace;
        this.typeName = typeName;
        this.selfLink = selfLink;
        this.creationTimestamp = creationTimestamp;
        this.resourceVersion = resourceVersion;
        this.endpointList = endpointList;
        this.planName = planName;
        this.authenticationService = authenticationService;
        this.status = status;
        this.uid = uid;
        this.labels = labels;
        this.annotations = annotations;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getType() {
        return typeName;
    }

    public List<io.enmasse.address.model.Endpoint> getEndpoints() {
        if (endpointList != null) {
            return Collections.unmodifiableList(endpointList);
        }
        return null;
    }

    public String getPlan() {
        return planName;
    }

    public String getUid() {
        return uid;
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public String getAnnotation(String key) {
        return annotations.get(key);
    }

    public void putAnnotation(String key, String value) {
        this.annotations.put(key, value);
    }

    public void putLabel(String key, String value) {
        this.labels.put(key, value);
    }

    public String getLabel(String key) {
        return labels.get(key);
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressSpace that = (AddressSpace) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{name=").append(name).append(",")
                .append("namespace=").append(namespace).append(",")
                .append("type=").append(typeName).append(",")
                .append("plan=").append(planName).append(",")
                .append("endpoints=").append(endpointList).append("}");
        return sb.toString();
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
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

    public static class Builder {
        private String name;
        private String namespace;
        private String selfLink;
        private String creationTimestamp;
        private String resourceVersion;

        private String type;
        private List<io.enmasse.address.model.Endpoint> endpointList = new ArrayList<>();
        private String plan;
        private AuthenticationService authenticationService = new AuthenticationService.Builder().build();
        private Status status = new Status(false);
        private String uid;
        private Map<String, String> labels = new HashMap<>();
        private Map<String, String> annotations = new HashMap<>();

        public Builder() {
        }

        public Builder(io.enmasse.address.model.AddressSpace addressSpace) {
            this.name = addressSpace.getName();
            this.namespace = addressSpace.getNamespace();
            this.type = addressSpace.getType();
            if (addressSpace.getEndpoints() != null) {
                this.endpointList = new ArrayList<>(addressSpace.getEndpoints());
            } else {
                this.endpointList = null;
            }
            this.plan = addressSpace.getPlan();
            this.status = new Status(addressSpace.getStatus());
            this.authenticationService = addressSpace.getAuthenticationService();
            this.uid = addressSpace.getUid();
            this.labels = new HashMap<>(addressSpace.getLabels());
            this.annotations = new HashMap<>(addressSpace.getAnnotations());
            this.selfLink = addressSpace.getSelfLink();
            this.creationTimestamp = addressSpace.getCreationTimestamp();
            this.resourceVersion = addressSpace.getResourceVersion();
        }

        public Map<String, String> getAnnotations() {
            return annotations;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setNamespace(String namespace) {
            this.namespace = namespace;
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

        public Builder setType(String addressSpaceType) {
            this.type = addressSpaceType;
            return this;
        }

        public Builder setUid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder setEndpointList(List<Endpoint> endpointList) {
            if (endpointList != null) {
                this.endpointList = new ArrayList<>(endpointList);
            } else {
                this.endpointList = null;
            }
            return this;
        }

        public Builder appendEndpoint(Endpoint endpoint) {
            this.endpointList.add(endpoint);
            return this;
        }

        public Builder setPlan(String plan) {
            this.plan = plan;
            return this;
        }

        public Builder setAuthenticationService(AuthenticationService authenticationService) {
            this.authenticationService = authenticationService;
            return this;
        }

        public Builder setStatus(Status status) {
            this.status = status;
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

        public AddressSpace build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(type, "type not set");
            Objects.requireNonNull(plan, "plan not set");
            Objects.requireNonNull(authenticationService, "authentication service not set");
            Objects.requireNonNull(status, "status not set");

            return new AddressSpace(name, namespace, type, selfLink, creationTimestamp, resourceVersion, endpointList, plan, authenticationService, status, uid, labels, annotations);
        }

        public String getNamespace() {
            return namespace;
        }

        public String getName() {
            return name;
        }

        public List<Endpoint> getEndpoints() {
            return endpointList;
        }

        public Status getStatus() {
            return status;
        }
    }
}
