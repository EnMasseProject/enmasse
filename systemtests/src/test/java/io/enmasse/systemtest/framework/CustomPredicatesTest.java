/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework;

import static io.enmasse.systemtest.utils.AssertionPredicate.from;
import static io.enmasse.systemtest.utils.AssertionPredicate.isNotPresent;
import static io.enmasse.systemtest.utils.AssertionPredicate.isPresent;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import io.enmasse.systemtest.TestTag;

@Tag(TestTag.FRAMEWORK)
public class CustomPredicatesTest {

    private static final Optional<String> empty = Optional.empty();
    private static final String text = "asdfg";
    private static final Optional<String> value = Optional.of(text);

    @Test
    void testAssertionPredicate() {
        isPresent(value).assertTrue("");
        isPresent(value).and(v -> v.equals(text)).assertTrue("");
        isPresent(value).or(v -> false).assertTrue("");

        isNotPresent(empty).assertTrue("");
        isNotPresent(empty).and(v -> v == null).assertTrue("");
        isNotPresent(empty).or(v -> {
            //this isn't executed
            return v.chars()!=null;
        }).assertTrue("");
        isNotPresent(empty).or(v -> false).assertTrue("");

        from("dummy", v -> v != null && v.length() > 0).assertTrue("");
    }

    @Test
    void testNegativeAssertionPredicate() {
        Assertions.assertThrows(AssertionFailedError.class, () -> {
            isNotPresent(value).assertTrue("expected to fail");
            });
        Assertions.assertThrows(AssertionFailedError.class, () -> {
            isPresent(value).negate().assertTrue("expected to fail");
            });


        Assertions.assertThrows(AssertionFailedError.class, () -> {
            isPresent(empty).assertTrue("expected to fail");
            });

        Assertions.assertThrows(AssertionFailedError.class, () -> {
            from(text, v -> v != null && v.length() == 0).assertTrue("");
            });
    }

}
