/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.condition;

import java.util.function.Predicate;
import java.util.stream.Stream;

public enum OpenShiftVersion {

    OCP3(v-> v < 1.13),
    OCP4(v -> v >= 1.13),
    WHATEVER(v -> true);

    private final Predicate<Double> versionChecker;

    OpenShiftVersion(Predicate<Double> versionChecker) {
        this.versionChecker = versionChecker;
    }

    public static OpenShiftVersion fromK8sVersion(double k8sVersion) {
        return Stream.of(OpenShiftVersion.values())
                .filter(v -> v.versionChecker.test(k8sVersion))
                .findFirst()
                .orElse(OCP3);
    }

    public enum Openshift4MinorVersion {
        OCP4_PRIOR_4_4(v -> v >= 1.13 && v<1.17),
        OCP4_AFTER_4_4(v -> v >= 1.17 && v<1.19),
        OCP4_AFTER_4_6(v -> v >= 1.19);

        private final Predicate<Double> versionChecker;

        Openshift4MinorVersion(Predicate<Double> versionChecker) {
            this.versionChecker = versionChecker;
        }

        public static Openshift4MinorVersion fromK8sVersion(double k8sVersion) {
            if (k8sVersion < 1.13) {
                throw new IllegalArgumentException("k8s version is from Openshift 3");
            }
            return Stream.of(Openshift4MinorVersion.values())
                    .filter(v -> v.versionChecker.test(k8sVersion))
                    .findFirst()
                    .orElse(OCP4_PRIOR_4_4);
        }


    }

}