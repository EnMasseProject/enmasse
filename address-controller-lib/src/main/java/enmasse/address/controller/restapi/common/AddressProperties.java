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

package enmasse.address.controller.restapi.common;

/**
 * API type representing an address and its properties
 */
public class AddressProperties {
    public boolean store_and_forward;
    public boolean multicast;
    public String flavor;

    public AddressProperties() {}

    public AddressProperties(boolean store_and_forward, boolean multicast, String flavor) {
        this.store_and_forward = store_and_forward;
        this.multicast = multicast;
        this.flavor = flavor;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressProperties that = (AddressProperties) o;

        if (store_and_forward != that.store_and_forward) return false;
        if (multicast != that.multicast) return false;
        return flavor != null ? flavor.equals(that.flavor) : that.flavor == null;
    }

    @Override
    public int hashCode() {
        int result = (store_and_forward ? 1 : 0);
        result = 31 * result + (multicast ? 1 : 0);
        result = 31 * result + (flavor != null ? flavor.hashCode() : 0);
        return result;
    }
}
