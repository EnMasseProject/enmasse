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
package io.enmasse.systemtest;

public class AddressSpace {
    private final String name;
    private final String namespace;
    private AddressSpaceType type;

    public AddressSpace(String name) {
        this.name = name;
        this.namespace = name;
        this.type = AddressSpaceType.STANDARD;
    }

    public AddressSpace(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
        this.type = AddressSpaceType.STANDARD;
    }

    public AddressSpace(String name, AddressSpaceType type) {
        this.name = name;
        this.namespace = name;
        this.type = type;
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
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

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("namespace=").append(namespace).append(",")
                .append("type=").append(type.toString().toLowerCase()).append("}")
                .toString();
    }
}
