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
package enmasse.controller.model;

/**
 * Represents a instance
 */
public final class InstanceId {
    private final String id;
    private final String namespace;

    private InstanceId(String id, String namespace) {
        this.id = id;
        this.namespace = namespace;
    }

    public String getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public static InstanceId withId(String idString) {
        return withIdAndNamespace(idString, "enmasse-" + idString);
    }

    public static InstanceId withIdAndNamespace(String idString, String namespace) {
        return new InstanceId(idString, namespace);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstanceId instanceId = (InstanceId) o;

        return id.equals(instanceId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
