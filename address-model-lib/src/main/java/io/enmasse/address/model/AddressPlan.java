/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class AddressPlan {
    private final String name;
    private final String displayName;
    private final int displayOrder;
    private final String shortDescription;
    private final String longDescription;
    private final String uuid;
    private final String addressType;
    private final List<ResourceRequest> requiredResources;

    private AddressPlan(String name, String displayName, int displayOrder, String shortDescription, String longDescription, String uuid, String addressType, List<ResourceRequest> requiredResources) {
        this.name = name;
        this.displayName = displayName;
        this.displayOrder = displayOrder;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.uuid = uuid;
        this.addressType = addressType;
        this.requiredResources = requiredResources;
    }


    public List<ResourceRequest> getRequiredResources() {
        return Collections.unmodifiableList(requiredResources);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public String getUuid() {
        return uuid;
    }

    public String getAddressType() {
        return addressType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressPlan that = (AddressPlan) o;

        if (!name.equals(that.name)) return false;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + uuid.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AddressPlan{" +
                "name='" + name + '\'' +
                ", uuid='" + uuid + '\'' +
                ", addressType='" + addressType + '\'' +
                ", requiredResources=" + requiredResources +
                '}';
    }

    public static class Builder {
        private String name;
        private String displayName;
        private int displayOrder = 0;
        private String shortDescription;
        private String longDescription;
        private String uuid;
        private String addressType;
        private List<ResourceRequest> requestedResources;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder setDisplayOrder(int displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public Builder setShortDescription(String shortDescription) {
            this.shortDescription = shortDescription;
            return this;
        }

        public Builder setLongDescription(String longDescription) {
            this.longDescription = longDescription;
            return this;
        }

        public Builder setUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder setRequestedResources(List<ResourceRequest> requestedResources) {
            this.requestedResources = requestedResources;
            return this;
        }

        public Builder setAddressType(String addressType) {
            this.addressType = addressType;
            return this;
        }

        private void setDefaults() {
            if (displayName == null) {
                displayName = name;
            }
            if (shortDescription == null) {
                shortDescription = displayName;
            }
            if (longDescription == null) {
                longDescription = shortDescription;
            }
            if (uuid == null) {
                uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
            }
        }

        public AddressPlan build() {
            setDefaults();
            Objects.requireNonNull(name);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(shortDescription);
            Objects.requireNonNull(longDescription);
            Objects.requireNonNull(uuid);
            Objects.requireNonNull(addressType);
            Objects.requireNonNull(requestedResources);

            return new AddressPlan(name, displayName, displayOrder, shortDescription, longDescription, uuid, addressType, requestedResources);
        }
    }
}
