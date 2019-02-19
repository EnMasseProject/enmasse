/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.validation;

import static org.hamcrest.CoreMatchers.not;

import java.util.Objects;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.Validator;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import io.enmasse.model.validation.DefaultValidator;

public final class ValidationMatchers {

    private ValidationMatchers() {}

    /**
     * Inversion of {@link #isValid()}.
     */
    public static <T> Matcher<T> isNotValid() {
        return not(isValid());
    }

    /**
     * Inversion of {@link #isValid(Validator)}.
     */
    public static <T> Matcher<T> isNotValid(final Validator validator) {
        return not(isValid(validator));
    }

    /**
     * Create a new matcher validating with the {@link DefaultValidator}.
     *
     * @return The new matcher.
     */
    public static <T> Matcher<T> isValid() {
        return isValid(DefaultValidator.validator());
    }

    /**
     * Create a new matcher validating with a {@link Validator}.
     *
     * @param validator The validator to use, must not be {@code null}.
     * @return The new matcher.
     */
    public static <T> Matcher<T> isValid(final Validator validator) {
        Objects.requireNonNull(validator);

        return new BaseMatcher<T>() {

            @Override
            public boolean matches(final Object item) {
                try {
                    DefaultValidator.validate(validator, item);
                    return true;
                } catch (ValidationException e) {
                    return false;
                }
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                try {
                    DefaultValidator.validate(item);
                } catch (ConstraintViolationException e) {
                    description.appendText(e.getMessage());
                }
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("no validation errors");
            }
        };
    }
}
