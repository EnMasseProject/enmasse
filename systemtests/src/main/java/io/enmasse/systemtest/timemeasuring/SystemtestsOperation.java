/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.timemeasuring;

public enum SystemtestsOperation {
    TEST_EXECUTION,
    CREATE_ADDRESS_SPACE,
    CREATE_USER,
    CREATE_ADDRESS,
    DELETE_ADDRESS_SPACE,
    DELETE_USER,
    UPDATE_USER,
    DELETE_ADDRESS,
    APPEND_ADDRESS,
    ADDRESS_WAIT_READY,
    ADDRESS_WAIT_PLAN_CHANGE,
    ADDRESS_WAIT_BROKER_DRAINED,
    UPDATE_ADDRESS,
    UPDATE_ADDRESS_SPACE,
    CREATE_SELENIUM_CONTAINER,
    DELETE_SELENIUM_CONTAINER,
    CREATE_IOT_CONFIG,
    DELETE_IOT_CONFIG,
    CREATE_IOT_PROJECT,
    DELETE_IOT_PROJECT;
}
