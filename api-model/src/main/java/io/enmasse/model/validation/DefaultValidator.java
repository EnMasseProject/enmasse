/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public final class DefaultValidator {

    private static final ValidatorFactory FACTORY;

    static {
        FACTORY = Validation.buildDefaultValidatorFactory();
    }

    private DefaultValidator() {}

    public static Validator validator() {
        return FACTORY.getValidator();
    }

    public static void validate(final Validator validator, final Object bean) {
        final Set<ConstraintViolation<Object>> result = validator.validate(bean);

        if (result.isEmpty()) {
            return;
        }

        throw new ConstraintViolationException(result);
    }

    public static void validate(final Object bean) {
        validate(validator(), bean);
    }

}
