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

/**
 * The different address types for a broker.
 */
public enum AddressType {
    QUEUE("queue"),
    TOPIC("topic");

    private final String name;
    AddressType(String name) {
        this.name = name;
    }

    public String value() {
        return this.name;
    }

    public static void validate(String type) {
        if (!QUEUE.name.equals(type) && !TOPIC.name.equals(type)) {
            throw new IllegalArgumentException("Unknown address type " + type);
        }
    }
}
