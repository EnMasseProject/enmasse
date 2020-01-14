/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TestTag {
    public static final String ISOLATED = "isolated";
    public static final String ISOLATED_STANDARD = "isolated-standard";
    public static final String ISOLATED_BROKER = "isolated-broker";
    public static final String SHARED_STANDARD = "shared-standard";
    public static final String SHARED_BROKERED = "shared-brokered";
    public static final String SHARED_MQTT = "shared-mqtt";
    public static final String SHARED_IOT = "shared-iot";
    public static final String ISOLATED_IOT = "isolated-iot";
    public static final String SOAK = "soak";
    public static final String NON_PR = "nonPR";
    public static final String UPGRADE = "upgrade";
    public static final String NONE_AUTH = "noneAuth";
    public static final String SMOKE = "smoke";
    public static final String ACCEPTANCE = "acceptance";
    public static final String OLM = "olm";
    public static final Set<String> SHARED_TAGS = new HashSet<>(Arrays.asList(SHARED_BROKERED, SHARED_STANDARD, SHARED_MQTT, SHARED_IOT));
    public static final Set<String> IOT_TAGS = new HashSet<>(Arrays.asList(SHARED_IOT, ISOLATED_IOT));
}
