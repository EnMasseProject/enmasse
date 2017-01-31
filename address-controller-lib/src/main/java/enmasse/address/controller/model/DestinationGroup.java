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

package enmasse.address.controller.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A destination group is a group of destinations having a shared group identifier.
 */
public class DestinationGroup {
    private final String groupId;
    private final Set<Destination> destinations;

    public DestinationGroup(String groupId, Set<Destination> destinations) {
        this.groupId = groupId;
        this.destinations = destinations;
    }

    public String getGroupId() {
        return groupId;
    }

    public Set<Destination> getDestinations() {
        return Collections.unmodifiableSet(destinations);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{groupId=").append(groupId).append(",")
                .append("destinations=").append(destinations).append("}");
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DestinationGroup that = (DestinationGroup) o;

        return groupId.equals(that.groupId);
    }

    @Override
    public int hashCode() {
        return groupId.hashCode();
    }

    public static class Builder {
        private String groupId;
        private Set<Destination> destinations = new LinkedHashSet<>();

        public Builder(String groupId) {
            this.groupId = groupId;
        }

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder destination(Destination destination) {
            this.destinations.add(destination);
            return this;
        }

        public DestinationGroup build() {
            return new DestinationGroup(groupId, destinations);
        }
    }
}
