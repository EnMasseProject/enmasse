/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This test suite is organized using different test profiles and those test profiles use a different set of tags.
 * <p>
 * This class defines all the tags used in the test suite.
 */
public class TestTag {
    public static final String ISOLATED = "isolated";
    public static final String ISOLATED_SHARED_INFRA = "isolated-shared-infra";
    public static final String SHARED = "shared";
    public static final String SHARED_IOT = "shared-iot";
    public static final String ISOLATED_IOT = "isolated-iot";
    public static final String SOAK = "soak";
    public static final String NON_PR = "nonPR";
    public static final String UPGRADE = "upgrade";
    public static final String SMOKE = "smoke";
    public static final String ACCEPTANCE = "acceptance";
    public static final String SCALE = "scale";
    public static final String OLM = "olm";
    public static final String FRAMEWORK = "framework";
    public static final Set<String> SHARED_TAGS = new HashSet<>(Arrays.asList(SHARED, SHARED_IOT));
    public static final Set<String> IOT_TAGS = new HashSet<>(Arrays.asList(SHARED_IOT, ISOLATED_IOT));
    public static final Set<String> SHARED_INFRA_TAGS = new HashSet<>(Collections.singletonList(ISOLATED_SHARED_INFRA));
}
