/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Policy {
    private Boolean enableVhostPolicy;

    public Boolean getEnableVhostPolicy() {
        return enableVhostPolicy;
    }

    public void setEnableVhostPolicy(Boolean enableVhostPolicy) {
        this.enableVhostPolicy = enableVhostPolicy;
    }

    @Override
    public String toString() {
        return "Policy{" +
                "enableVhostPolicy=" + enableVhostPolicy +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Policy policy = (Policy) o;
        return Objects.equals(enableVhostPolicy, policy.enableVhostPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableVhostPolicy);
    }
}
