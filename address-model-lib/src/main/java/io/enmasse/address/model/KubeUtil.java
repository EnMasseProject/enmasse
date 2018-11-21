/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.config.AnnotationKeys;

/**
 * Various static utilities that don't belong in a specific place
 */
public class KubeUtil {
    private static int MAX_KUBE_NAME = 63 - 3; // max length of identifier - space for pod identifier
    private static final String addressPattern = "[^a-z0-9\\-]";
    private static final String usernamePattern = "[^a-z0-9\\-.@_]";

    public static String sanitizeName(String name) {
        return sanitizeWithPattern(name, addressPattern);
    }

    public static String sanitizeUserName(String name) {
        return sanitizeWithPattern(name, usernamePattern);
    }

    private static String sanitizeWithPattern(String value, String pattern) {
        if (value == null) {
            return null;
        }

        String clean = value.toLowerCase().replaceAll(pattern, "");
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

    public static String getAddressSpaceCaSecretName(AddressSpace addressSpace) {
        return sanitizeName("ca-" + addressSpace.getName() + "." + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID));
    }

    public static String getAddressSpaceExternalCaSecretName(AddressSpace addressSpace) {
        return sanitizeName("route-ca-" + addressSpace.getName() + "." + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID));
    }

    public static String getAddressSpaceSaName(AddressSpace addressSpace) {
        return "sa-" + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
    }

    public static String getAddressSpaceServiceName(String serviceName, AddressSpace addressSpace) {
        return serviceName + "-" + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
    }

    public static String getAddressSpaceServiceHost(String serviceName, String namespace, AddressSpace addressSpace) {
        return getAddressSpaceServiceName(serviceName, addressSpace) + "." + namespace + ".svc";
    }

    public static String getExternalCertSecretName(String serviceName, AddressSpace addressSpace) {
        return "external-certs-" + getAddressSpaceServiceName(serviceName, addressSpace);
    }

    public static String getAddressSpaceRouteName(String routeName, AddressSpace addressSpace) {
        return routeName + "-" + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
    }

    public static void validateName(String name) {
        if (name == null) {
            return;
        }

        if (name.length() > MAX_KUBE_NAME) {
            throw new IllegalArgumentException("Name length is longer than " + MAX_KUBE_NAME + " characters");
        }

        if (name.matches(addressPattern)) {
            throw new IllegalArgumentException("Illegal characters found in " + name + ". Must not match " + addressPattern);
        }
    }

    public static String getNetworkPolicyName(AddressSpace addressSpace) {
        return addressSpace.getName() + "." + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
    }
}
