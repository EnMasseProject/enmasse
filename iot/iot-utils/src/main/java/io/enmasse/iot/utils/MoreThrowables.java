/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.utils;

import java.util.Optional;

import com.google.common.base.Throwables;

public final class MoreThrowables {

    private MoreThrowables() {}

    /**
     * Find a cause of provided type.
     *
     * @param <T> The cause type to look for.
     * @param e The throwable to search.
     * @param clazz The class to look for.
     * @return An {@link Optional} with the cause of type {@code <T>}, should that exist in the cause
     *         chain. Otherwise, or of the throwable provided is {@code null}, {@link Optional#empty()}
     *         will be returned.
     */
    public static <T extends Throwable> Optional<T> causeOf(final Throwable e, final Class<T> clazz) {
        if (e == null) {
            return Optional.empty();
        }
        return Throwables.getCausalChain(e)
                .stream()
                .filter(clazz::isInstance)
                .findFirst()
                .map(clazz::cast);
    }

    /**
     * Check if the throwable has a cause of the provided type.
     *
     * @param <T> The cause type to look for.
     * @param e The throwable to search.
     * @param clazz The class to look for.
     * @return {@code true} if the cause chain contains the requested exception, {@code false}
     *         otherwise. If the throwable to check is {@code null}, false will be returned as well.
     */
    public static <T extends Throwable> boolean hasCauseOf(final Throwable e, final Class<T> clazz) {
        return causeOf(e, clazz).isPresent();
    }
}
