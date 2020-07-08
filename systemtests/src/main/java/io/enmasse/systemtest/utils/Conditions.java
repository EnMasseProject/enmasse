/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.utils;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;

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
            final String conditionType
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
            final String conditionType,
            boolean expected
    ) {

        var access = client.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName());
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
                var state = statusSection(access.get());
                return String.format("Failed to detect condition '%s' of '%s/%s' (%s/%s) as '%s': %s",
                        conditionType,
                        resource.getMetadata().getNamespace(),
                        resource.getMetadata().getName(),
                        resource.getApiVersion(), resource.getKind(),
                        expectedString,
                        state.map(Serialization::asJson).orElse("<no status>")
                );
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

    static String conditionStatus(final Object current, final String conditionType) {
        try {
            var status = statusSection(current).get();
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

}
