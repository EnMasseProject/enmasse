/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import io.enmasse.address.model.StringOrSecretSelector;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class StringOrSecretSelectorValidator implements ConstraintValidator<StringOrSecretSelectorSpec, StringOrSecretSelector> {

    @Override
    public boolean isValid(StringOrSecretSelector selector, ConstraintValidatorContext context) {

        if (selector == null) {
            return true;
        }

        return (selector.getValue() == null || selector.getValueFromSecret() == null);
    }
}

