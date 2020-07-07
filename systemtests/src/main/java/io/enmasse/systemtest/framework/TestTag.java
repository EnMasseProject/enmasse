/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework;

/**
 * This test suite is organized using different test profiles and those test profiles use a different set of tags.
 *
 * This class defines all the tags used in the test suite.
 */
public class TestTag {
    public static final String SYSTEMTEST = "systemtests";
    public static final String SOAK = "soak";
    public static final String UPGRADE = "upgrade";
    public static final String ACCEPTANCE = "acceptance";
    public static final String SCALE = "scale";
    public static final String FRAMEWORK = "framework";
    public static final String IOT = "iot";
}
