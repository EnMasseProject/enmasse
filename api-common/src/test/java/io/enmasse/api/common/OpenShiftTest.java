/*
 * Copyright 2021, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.common;

import io.fabric8.kubernetes.client.VersionInfo;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class OpenShiftTest {

    @Test
    public void testOpenShift4() {

        VersionInfo openshift3 = new VersionInfo.Builder().withMajor("1").withMinor("11").build();
        assertFalse(OpenShift.isOpenShift4(openshift3));
        VersionInfo openshift4 = new VersionInfo.Builder().withMajor("1").withMinor("21").build();
        assertTrue(OpenShift.isOpenShift4(openshift4));
        VersionInfo openshift4WithPlus = new VersionInfo.Builder().withMajor("1").withMinor("21+").build();
        assertTrue(OpenShift.isOpenShift4(openshift4WithPlus));
    }

}