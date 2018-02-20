/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

final public class NamespaceInfo {
    private final String configName;
    private final String addressSpace;
    private final String namespace;
    private final String createdBy;

    public NamespaceInfo(String configName, String addressSpace, String namespace, String createdBy) {
        this.configName = configName;
        this.addressSpace = addressSpace;
        this.namespace = namespace;
        this.createdBy = createdBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getAddressSpace() {
        return addressSpace;
    }

    public String getConfigName() {
        return configName;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{addressSpace=").append(addressSpace).append(",")
                .append("namespace=").append(namespace).append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NamespaceInfo that = (NamespaceInfo) o;

        return addressSpace.equals(that.addressSpace);
    }

    @Override
    public int hashCode() {
        return addressSpace.hashCode();
    }
}
