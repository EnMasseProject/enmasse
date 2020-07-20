/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.utils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public final class Conditions {

    private Conditions() {
    }

    /**
     * Create a condition, checking for a Kubernetes condition.
     *
     * @param client The client used to fetch an update of the resource.
     * @param resource The resource to check.
     * @param conditionType The of the condition to check.
     * @param <T> Type of resource.
     * @param <L> List type of the resource.
     * @param <D> The "doneable" of the resource.
     * @return a boolean condition, which can evaluate if the Kubernetes resource condition has the expected state.
     */
    public static <T extends HasMetadata, L, D> BooleanSupplier condition(
            final MixedOperation<T, L, D, Resource<T, D>> client,
            final T resource,
            final Object conditionType
    ) {
        return condition(client, resource, conditionType, true);
    }

    /**
     * Create a condition, checking for a Kubernetes condition.
     *
     * @param client The client used to fetch an update of the resource.
     * @param resource The resource to check.
     * @param conditionType The of the condition to check.
     * @param expected The expected status.
     * @param <T> Type of resource.
     * @param <L> List type of the resource.
     * @param <D> The "doneable" of the resource.
     * @return a boolean condition, which can evaluate if the Kubernetes resource condition has the expected state.
     */
    public static <T extends HasMetadata, L, D> BooleanSupplier condition(
            final MixedOperation<T, L, D, Resource<T, D>> client,
            final T resource,
            final Object conditionType,
            final boolean expected
    ) {
        return condition(
                client
                        .inNamespace(resource.getMetadata().getNamespace())
                        .withName(resource.getMetadata().getName()),
                conditionType,
                expected);
    }

    /**
     * Create a condition, checking for a Kubernetes condition.
     *
     * @param access The resource to check.
     * @param conditionType The of the condition to check.
     * @param <T> Type of resource.
     * @param <D> The "doneable" of the resource.
     * @return a boolean condition, which can evaluate if the Kubernetes resource condition has the expected state.
     */
    public static <T extends HasMetadata, D> BooleanSupplier condition(
            final Resource<T, D> access,
            final Object conditionType
    ) {
        return condition(access, conditionType, true);
    }

    /**
     * Create a condition, checking for a Kubernetes condition.
     *
     * @param access The resource to check.
     * @param conditionType The of the condition to check.
     * @param expected The expected status.
     * @param <T> Type of resource.
     * @param <D> The "doneable" of the resource.
     * @return a boolean condition, which can evaluate if the Kubernetes resource condition has the expected state.
     */
    public static <T extends HasMetadata, D> BooleanSupplier condition(
            final Resource<T, D> access,
            final Object conditionType,
            final boolean expected
    ) {

        var expectedString = expected ? "True" : "False";

        return new BooleanSupplier() {

            @Override
            public boolean getAsBoolean() {
                var current = access.get();
                var state = conditionStatus(current, conditionType);
                return expectedString.equals(state);
            }

            @Override
            public String toString() {
                return reasonFromStatus(String.format("Failed to detect condition '%s' as '%s'", conditionType, expectedString), access);
            }
        };

    }

    private static Optional<Object> statusSection(final Object current) {
        try {
            return Optional.ofNullable(current.getClass().getMethod("getStatus").invoke(current));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static String conditionStatus(final Object current, final Object conditionType) {
        try {
            var statusSection = statusSection(current);
            if (statusSection.isEmpty()) {
                return null;
            }
            var status = statusSection.get();
            var conditions = status.getClass().getMethod("getConditions").invoke(status);
            for (Object o : (Iterable<?>) conditions) {
                var clazz = o.getClass();
                var name = clazz.getMethod("getType").invoke(o);
                if (conditionType.equals(name)) {
                    return (String) clazz.getMethod("getStatus").invoke(o);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static BooleanSupplier gone(final Resource<? extends HasMetadata, ?> resource, final String uid) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                var r = resource.get();
                if (r == null) {
                    return true;
                }
                if (uid == null) {
                    return false;
                }
                return uid.equals(r.getMetadata().getUid());
            }

            @Override
            public String toString() {
                return reasonFromStatus("Resource should be gone, but is not", resource);
            }
        };
    }

    public static BooleanSupplier gone(final Resource<? extends HasMetadata, ?> resource) {
        return gone(resource, null);
    }

    private static String reasonFromStatus(final String message, final Resource<? extends HasMetadata, ?> access) {
        var resource = access.get();
        var state = statusSection(resource);
        return String.format("%s: '%s/%s' (%s/%s): %s",
                message,
                resource.getMetadata().getNamespace(),
                resource.getMetadata().getName(),
                resource.getApiVersion(), resource.getKind(),
                state.map(Serialization::toYaml).orElse("<no status>")
        );
    }

    public static BooleanSupplier not(final BooleanSupplier condition) {
        Objects.requireNonNull(condition);
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return !condition.getAsBoolean();
            }

            @Override
            public String toString() {
                return "not: " + super.toString();
            }
        };
    }

}
