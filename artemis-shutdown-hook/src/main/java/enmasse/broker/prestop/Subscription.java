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

package enmasse.broker.prestop;

/**
 * TODO: Description
 */
public class Subscription {
    private final String clientId;
    private final String name;
    private final boolean durable;
    public Subscription(String clientId, String name, boolean durable) {
        this.clientId = clientId;
        this.name = name;
        this.durable = durable;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public boolean isDurable() {
        return durable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subscription that = (Subscription) o;

        if (durable != that.durable) return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        int result = clientId != null ? clientId.hashCode() : 0;
        result = 31 * result + name.hashCode();
        result = 31 * result + (durable ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "clientId='" + clientId + '\'' +
                ", name='" + name + '\'' +
                ", durable=" + durable +
                '}';
    }
}
