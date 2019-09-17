/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import io.enmasse.address.model.AddressSpecForwarder;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class AddressForwarderNameValidator implements ConstraintValidator<AddressForwarderName, AddressSpecForwarder> {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9]+");

    @Override
    public boolean isValid(AddressSpecForwarder forwarder, ConstraintValidatorContext context) {

        if (forwarder == null) {
            return true;
        }

        if (forwarder.getName() == null) {
            return false;
        }

        return NAME_PATTERN.matcher(forwarder.getName()).matches();
    }
}

