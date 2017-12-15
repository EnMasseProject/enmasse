/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.address.model;

import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.Plan;

import java.util.*;

/**
 * An EnMasse AddressSpace address-space.
 */
public class AddressSpace {
    private final String name;
    private final String namespace;
    private final AddressSpaceType type;
    private final List<io.enmasse.address.model.Endpoint> endpointList;
    private final Plan plan;
    private final AuthenticationService authenticationService;
    private final Status status;
    private final String uid;
    private final String createdBy;

    private AddressSpace(String name, String namespace, AddressSpaceType type, List<Endpoint> endpointList, Plan plan, AuthenticationService authenticationService, Status status, String uid, String createdBy) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
        this.endpointList = endpointList;
        this.plan = plan;
        this.authenticationService = authenticationService;
        this.status = status;
        this.uid = uid;
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public AddressSpaceType getType() {
        return type;
    }

    public List<io.enmasse.address.model.Endpoint> getEndpoints() {
        if (endpointList != null) {
            return Collections.unmodifiableList(endpointList);
        }
        return null;
    }

    public Plan getPlan() {
        return plan;
    }

    public String getUid() {
        return uid;
    }

    public Status getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
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
                .append("type=").append(type.getName()).append(",")
                .append("plan=").append(plan.getName()).append("}");
        return sb.toString();
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public static class Builder {
        private String name;
        private String namespace;
        private AddressSpaceType type;
        private List<io.enmasse.address.model.Endpoint> endpointList = new ArrayList<>();
        private Plan plan;
        private AuthenticationService authenticationService = new AuthenticationService.Builder().build();
        private Status status = new Status(false);
        private String uid;
        private String createdBy;

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
            this.createdBy = addressSpace.getCreatedBy();
        }

        public Builder setName(String name) {
            this.name = name;
            if (this.namespace == null) {
                this.namespace = "enmasse-" + name;
            }
            return this;
        }

        public Builder setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder setType(AddressSpaceType type) {
            this.type = type;
            if (this.plan == null) {
                this.plan = type.getDefaultPlan();
            }
            return this;
        }

        public Builder setUid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder setEndpointList(List<io.enmasse.address.model.Endpoint> endpointList) {
            if (endpointList != null) {
                this.endpointList = new ArrayList<>(endpointList);
            } else {
                this.endpointList = null;
            }
            return this;
        }

        public Builder appendEndpoint(io.enmasse.address.model.Endpoint endpoint) {
            this.endpointList.add(endpoint);
            return this;
        }

        public Builder setPlan(Plan plan) {
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

        public Builder setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public AddressSpace build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(namespace, "namespace not set");
            Objects.requireNonNull(type, "type not set");
            Objects.requireNonNull(authenticationService, "authentication service not set");
            Objects.requireNonNull(plan, "plan not set");
            Objects.requireNonNull(status, "status not set");
            return new AddressSpace(name, namespace, type, endpointList, plan, authenticationService, status, uid, createdBy);
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
