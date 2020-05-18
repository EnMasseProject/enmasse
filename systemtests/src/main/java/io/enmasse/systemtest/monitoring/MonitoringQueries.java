/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.monitoring;

public interface MonitoringQueries {
    String ENMASSE_ADDRESS_SPACES_NOT_READY = "enmasse_address_space_status_not_ready";
    String ENMASSE_ADDRESS_SPACES_READY = "enmasse_address_space_status_ready";
}
