/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;

/**
 * Various static utilities that don't belong in a specific place
 */
public final class KubeUtil {
    private static final int MAX_KUBE_NAME = 63 - 3; // max length of identifier - space for pod identifier
    private static final Pattern addressPattern = Pattern.compile("[^a-z0-9\\-]");
    private static final Pattern usernamePattern = Pattern.compile("[^a-z0-9\\-.@_]");

    public KubeUtil() {
    }

    public static String sanitizeName(String name) {
        return sanitizeWithPattern(name, addressPattern);
    }

    public static String sanitizeUserName(String name) {
        return sanitizeWithPattern(name, usernamePattern);
    }

    private static String sanitizeWithPattern(String value, Pattern pattern) {
        if (value == null) {
            return null;
        }

        String clean = pattern
                        .matcher(value.toLowerCase())
                        .replaceAll("");

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
        return sanitizeName("ca-" + addressSpace.getMetadata().getName() + "." + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID));
    }

    public static String getAddressSpaceExternalCaSecretName(AddressSpace addressSpace) {
        return sanitizeName("route-ca-" + addressSpace.getMetadata().getName() + "." + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID));
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

    public static String getAddressSpaceRealmName(AddressSpace addressSpace) {
        return KubeUtil.sanitizeName(addressSpace.getMetadata().getNamespace() + "-" + addressSpace.getMetadata().getName());
    }

    public static String getAdminDeploymentName(AddressSpace addressSpace) {
        return "admin." + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
    }

    public static String getAgentDeploymentName(AddressSpace addressSpace) {
        return "agent." + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
    }

    public static String getRouterSetName(AddressSpace addressSpace) {
        return "qdrouterd-" + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
    }

    public static String getBrokeredBrokerSetName(AddressSpace addressSpace) {
        return "broker." + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
    }

    public static <T extends HasMetadata> T lookupResource(Class<T> clazz, String kind, String name, List<HasMetadata> items) {
        for (HasMetadata item : items) {
            if (item != null && item.getKind().equals(kind) && item.getMetadata().getName().equals(name)) {
                return clazz.cast(item);
            }
        }
        throw new IllegalStateException("Unable to find resource of kind '" + kind + "' and name '" + name + "'");
    }


    public static void applyPodTemplate(PodTemplateSpec actual, PodTemplateSpec desired) {
        if (desired.getMetadata() != null && desired.getMetadata().getLabels() != null) {
            Map<String, String> labels = new HashMap<>(desired.getMetadata().getLabels());
            if (actual.getMetadata().getLabels() != null) {
                labels.putAll(actual.getMetadata().getLabels());
            }
            actual.getMetadata().setLabels(labels);
        }

        if (desired.getSpec() != null) {

            PodSpec podSpec = desired.getSpec();
            PodSpec actualPodSpec = actual.getSpec();

            if (podSpec.getPriorityClassName() != null) {
                actualPodSpec.setPriorityClassName(podSpec.getPriorityClassName());
            }

            if (podSpec.getAffinity() != null) {
                actualPodSpec.setAffinity(podSpec.getAffinity());
            }

            if (podSpec.getTolerations() != null) {
                actualPodSpec.setTolerations(podSpec.getTolerations());
            }

            for (Container desiredContainer : podSpec.getContainers()) {
                for (Container actualContainer : actualPodSpec.getContainers()) {
                    if (actualContainer.getName().equals(desiredContainer.getName())) {
                        if (desiredContainer.getResources() != null) {
                            actualContainer.setResources(desiredContainer.getResources());
                        }
                    }
                }
            }
        }
    }


    public static void validateName(String name) {
        if (name == null) {
            return;
        }

        if (name.length() > MAX_KUBE_NAME) {
            throw new IllegalArgumentException("Name length is longer than " + MAX_KUBE_NAME + " characters");
        }

        if (addressPattern.matcher(name).matches()) {
            throw new IllegalArgumentException("Illegal characters found in " + name + ". Must not match " + addressPattern);
        }
    }

    public static String getNetworkPolicyName(AddressSpace addressSpace) {
        return addressSpace.getMetadata().getName() + "." + addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
    }

    public static boolean isNameValid(final String name) {
        // FIXME: invert exception logic, this should be primary and throwing exceptions secondary
        try {
            validateName(name);
            return true;
        } catch ( Exception e ) {
            return false;
        }
    }
}
