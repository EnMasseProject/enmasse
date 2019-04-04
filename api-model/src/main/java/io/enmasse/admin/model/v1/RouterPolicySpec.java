/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs = {@BuildableReference(AbstractWithAdditionalProperties.class)},
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"maxConnections", "maxConnectionsPerUser", "maxConnectionsPerHost", "maxSessionsPerConnection", "maxSendersPerConnection", "maxReceiversPerConnection"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterPolicySpec {
    private Integer maxConnections;
    private Integer maxConnectionsPerUser;
    private Integer maxConnectionsPerHost;
    private Integer maxSessionsPerConnection;
    private Integer maxSendersPerConnection;
    private Integer maxReceiversPerConnection;

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer getMaxConnectionsPerUser() {
        return maxConnectionsPerUser;
    }

    public void setMaxConnectionsPerUser(Integer maxConnectionsPerUser) {
        this.maxConnectionsPerUser = maxConnectionsPerUser;
    }

    public Integer getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(Integer maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public Integer getMaxSessionsPerConnection() {
        return maxSessionsPerConnection;
    }

    public void setMaxSessionsPerConnection(Integer maxSessionsPerConnection) {
        this.maxSessionsPerConnection = maxSessionsPerConnection;
    }

    public Integer getMaxSendersPerConnection() {
        return maxSendersPerConnection;
    }

    public void setMaxSendersPerConnection(Integer maxSendersPerConnection) {
        this.maxSendersPerConnection = maxSendersPerConnection;
    }

    public Integer getMaxReceiversPerConnection() {
        return maxReceiversPerConnection;
    }

    public void setMaxReceiversPerConnection(Integer maxReceiversPerConnection) {
        this.maxReceiversPerConnection = maxReceiversPerConnection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouterPolicySpec that = (RouterPolicySpec) o;
        return Objects.equals(maxConnections, that.maxConnections) &&
                Objects.equals(maxConnectionsPerUser, that.maxConnectionsPerUser) &&
                Objects.equals(maxConnectionsPerHost, that.maxConnectionsPerHost) &&
                Objects.equals(maxSessionsPerConnection, that.maxSessionsPerConnection) &&
                Objects.equals(maxSendersPerConnection, that.maxSendersPerConnection) &&
                Objects.equals(maxReceiversPerConnection, that.maxReceiversPerConnection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxConnections, maxConnectionsPerUser, maxConnectionsPerHost, maxSessionsPerConnection, maxSendersPerConnection, maxReceiversPerConnection);
    }

    @Override
    public String toString() {
        return "RouterPolicySpec{" +
                "maxConnections=" + maxConnections +
                ", maxConnectionsPerUser=" + maxConnectionsPerUser +
                ", maxConnectionsPerHost=" + maxConnectionsPerHost +
                ", maxSessionsPerConnection=" + maxSessionsPerConnection +
                ", maxSendersPerConnection=" + maxSendersPerConnection +
                ", maxReceiversPerConnection=" + maxReceiversPerConnection +
                '}';
    }
}
