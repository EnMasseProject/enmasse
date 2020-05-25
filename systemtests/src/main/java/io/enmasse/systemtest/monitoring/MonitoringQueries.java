/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.monitoring;

public interface MonitoringQueries {
    String ENMASSE_ADDRESS_SPACES_NOT_READY = "enmasse_address_space_status_not_ready";
    String ENMASSE_ADDRESS_SPACES_READY = "enmasse_address_space_status_ready";
    String ENMASSE_ADDRESS_READY_TOTAL = "enmasse_addresses_ready_total";
    String ENMASSE_ADDRESS_NOT_READY_TOTAL = "enmasse_addresses_not_ready_total";
    String ENMASSE_ADDRESS_CONFIGURING_TOTAL = "enmasse_addresses_configuring_total";
    String ENMASSE_ARTEMIS_DURABLE_MESSAGE_COUNT = "enmasse_artemis_durable_message_count";

    String ENMASSE_ADDRESSSPACEPLAN_INFO = "enmasse_addressspaceplan_info";
    String ENMASSE_ADDRESSPLAN_INFO = "enmasse_addressplan_info";

    String ENMASSE_AUTHENTICATION_SERVICE_READY = "enmasse_authentication_service_ready";
    String ENMASSE_COMPONENT_HEALTH = "enmasse_component_health";
    String ENMASSE_VERSION = "enmasse_version";
}
