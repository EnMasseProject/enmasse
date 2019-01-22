/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.enmasse.address.model.AddressSpace;

public class AddressSpaceNameValidator implements ConstraintValidator<AddressSpaceName, AddressSpace> {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z]*[a-z0-9\\-]*[a-z0-9]*");

    @Override
    public boolean isValid(AddressSpace addressSpace, ConstraintValidatorContext context) {

        if ( addressSpace == null ) {
            // if the object to check is null, we ignore it
            return true;
        }

        if ( addressSpace.getMetadata() == null ) {
            // will be checked by annotation on metadata
            return true;
        }

        if ( addressSpace.getMetadata().getName() == null ) {
            // we do require a name
            return false;
        }

        return NAME_PATTERN.matcher(addressSpace.getMetadata().getName()).matches();
    }
}

