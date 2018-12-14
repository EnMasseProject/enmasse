/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package enmasse.broker.prestop;

import java.util.Optional;

public class SubscriptionInfo {
    private final QueueInfo queueInfo;
    private final Optional<DivertInfo> divertInfo;
    private final Optional<ConnectorInfo> connectorInfo;

    public SubscriptionInfo(QueueInfo queueInfo, Optional<DivertInfo> divertInfo, Optional<ConnectorInfo> connectorInfo) {
        this.queueInfo = queueInfo;
        this.divertInfo = divertInfo;
        this.connectorInfo = connectorInfo;
    }

    public QueueInfo getQueueInfo() {
        return queueInfo;
    }

    public Optional<DivertInfo> getDivertInfo() {
        return divertInfo;
    }

    public Optional<ConnectorInfo> getConnectorInfo() {
        return connectorInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionInfo that = (SubscriptionInfo) o;

        if (!queueInfo.equals(that.queueInfo)) return false;
        if (!divertInfo.equals(that.divertInfo)) return false;
        return connectorInfo.equals(that.connectorInfo);
    }

    @Override
    public int hashCode() {
        int result = queueInfo.hashCode();
        result = 31 * result + divertInfo.hashCode();
        result = 31 * result + connectorInfo.hashCode();
        return result;
    }
}
