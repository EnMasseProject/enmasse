/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

/**
 * Varioius static utilities that don't belong in a specific place
 */
public class KubeUtil {
    public static String sanitizeName(String name) {
        String replaced = name.toLowerCase().replaceAll("[^a-z0-9]", "-");
        if (replaced.startsWith("-")) {
            replaced = replaced.replaceFirst("-", "1");
        }
        if (replaced.endsWith("-")) {
            replaced = replaced.substring(0, replaced.length() - 2) + "1";
        }
        return replaced;
    }

    public static String getAddressSpaceCaSecretName(String namespace) {
        return sanitizeName("addressspace-" + namespace + "-ca");
    }
}
