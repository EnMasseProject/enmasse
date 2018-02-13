/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class AddressSpacePlan {
    private final String name;
    private final String displayName;
    private final int displayOrder;
    private final String shortDescription;
    private final String longDescription;
    private final String uuid;
    private final String addressSpaceType;
    private final List<ResourceAllowance> allowedResources;
    private final List<String> addressPlans;
    private final Map<String, String> annotations;

    private AddressSpacePlan(String name, String displayName, int displayOrder, String shortDescription, String longDescription, String uuid, String addressSpaceType, List<ResourceAllowance> allowedResources, List<String> addressPlans, Map<String, String> annotations) {
        this.name = name;
        this.displayName = displayName;
        this.displayOrder = displayOrder;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.uuid = uuid;
        this.addressSpaceType = addressSpaceType;
        this.allowedResources = allowedResources;
        this.addressPlans = addressPlans;
        this.annotations = annotations;
    }


    public List<ResourceAllowance> getResources() {
        return Collections.unmodifiableList(allowedResources);
    }

    public List<String> getAddressPlans() {
        return Collections.unmodifiableList(addressPlans);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAddressSpaceType() {
        return addressSpaceType;
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

    public Map<String, String> getAnnotations() {
        return Collections.unmodifiableMap(annotations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressSpacePlan that = (AddressSpacePlan) o;

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
        return "AddressSpacePlan{" +
                "name='" + name + '\'' +
                ", uuid='" + uuid + '\'' +
                ", allowedResources=" + allowedResources +
                ", addressPlans=" + addressPlans +
                '}';
    }

    public static class Builder {
        private String name;
        private String displayName;
        private int displayOrder;
        private String shortDescription;
        private String longDescription;
        private String uuid;
        private String addressSpaceType;
        private List<ResourceAllowance> allowedResources;
        private List<String> addressPlans;
        private Map<String, String> annotations = new HashMap<>();

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

        public Builder setAddressSpaceType(String addressSpaceType) {
            this.addressSpaceType = addressSpaceType;
            return this;
        }

        public Builder setResources(List<ResourceAllowance> allowedResources) {
            this.allowedResources = allowedResources;
            return this;
        }

        public Builder setAddressPlans(List<String> addressPlans) {
            this.addressPlans = addressPlans;
            return this;
        }

        public Builder setAnnotations(Map<String, String> annotations) {
            this.annotations = new HashMap<>(annotations);
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

        public AddressSpacePlan build() {
            setDefaults();
            Objects.requireNonNull(name);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(shortDescription);
            Objects.requireNonNull(longDescription);
            Objects.requireNonNull(uuid);
            Objects.requireNonNull(addressSpaceType);
            Objects.requireNonNull(allowedResources);
            Objects.requireNonNull(addressPlans);
            Objects.requireNonNull(annotations);

            return new AddressSpacePlan(name, displayName, displayOrder, shortDescription, longDescription, uuid, addressSpaceType, allowedResources, addressPlans, annotations);
        }
    }
}
