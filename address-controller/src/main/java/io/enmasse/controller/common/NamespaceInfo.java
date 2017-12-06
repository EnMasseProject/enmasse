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
package io.enmasse.controller.common;

final public class NamespaceInfo {
    private final String addressSpace;
    private final String namespace;
    private final String createdBy;

    public NamespaceInfo(String addressSpace, String namespace, String createdBy) {
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
