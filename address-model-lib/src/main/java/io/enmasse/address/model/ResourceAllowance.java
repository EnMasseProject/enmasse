/*
 * Copyright 2018 Red Hat Inc.
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

public class ResourceAllowance {
    private final String resourceName;
    private final double min;
    private final double max;

    public ResourceAllowance(String resourceName, double min, double max) {
        this.resourceName = resourceName;
        this.min = min;
        this.max = max;
    }

    public String getResourceName() {
        return resourceName;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
