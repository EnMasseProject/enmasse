/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

/**
 * Varioius static utilities that don't belong in a specific place
 */
public class KubeUtil {
    private static int MAX_KUBE_NAME = 63 - 3; // max length of identifier - space for pod identifier
    public static String sanitizeName(String name) {
        String clean = name.toLowerCase().replaceAll("[^a-z0-9\\-]", "");
        if (clean.startsWith("-")) {
            clean = clean.replaceFirst("-", "1");
        }

        if (clean.length() > MAX_KUBE_NAME) {
            clean = clean.substring(0, MAX_KUBE_NAME);
        }

        if (clean.endsWith("-")) {
            clean = clean.substring(0, clean.length() - 1) + "1";
        }
        return clean;
    }

    public static String sanitizeWithUuid(String name, String uuid) {
        name = sanitizeName(name);
        if (name.length() + uuid.length() + 1 > MAX_KUBE_NAME) {
            name = name.substring(0, MAX_KUBE_NAME - uuid.length() - 1);
        }
        name += "-" + uuid;
        return name;
    }

    public static String getAddressSpaceCaSecretName(String namespace) {
        return "addressspace-" + namespace + "-ca";
    }
}
