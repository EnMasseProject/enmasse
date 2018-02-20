/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

/**
 * Varioius static utilities that don't belong in a specific place
 */
public class KubeUtil {
    public static String sanitizeName(String name) {
        String clean = name.toLowerCase().replaceAll("[^a-z0-9\\-]", "");
        if (clean.startsWith("-")) {
            clean = clean.replaceFirst("-", "1");
        }

        if (clean.length() > 63) {
            clean = clean.substring(0, 63);
        }

        if (clean.endsWith("-")) {
            clean = clean.substring(0, clean.length() - 2) + "1";
        }
        return clean;
    }

    public static String getAddressSpaceCaSecretName(String namespace) {
        return "addressspace-" + namespace + "-ca";
    }
}
