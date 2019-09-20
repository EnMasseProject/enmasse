/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.Arrays;
import java.util.List;

public class TestTag {
    public static final String ISOLATED = "isolated";
    public static final String ISOLATED_STANDARD = "isolated-standard";
    public static final String ISOLATED_BROKER = "isolated-broker";
    public static final String SHARED_STANDARD = "shared-standard";
    public static final String SHARED_BROKERED = "shared-brokered";
    public static final String SHARED_MQTT = "shared-mqtt";
    public static final String SHARED_IOT = "shared-iot";
    public static final String ISOLATED_IOT = "isolated-iot";
    public static final String MARATHON = "marathon";
    public static final String NON_PR = "nonPR";
    public static final String UPGRADE = "upgrade";
    public static final String NONE_AUTH = "noneAuth";
    public static final String SMOKE = "smoke";
    public static final String OLM = "olm";
    public static final String ACCEPTANCE = "acceptance";
    public static final List<String> SHARED_TAGS = Arrays.asList(SHARED_BROKERED, SHARED_STANDARD, SHARED_MQTT);
}
