/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import io.enmasse.address.model.AddressSpaceSpecConnector;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class AddressSpaceConnectorNameValidator implements ConstraintValidator<AddressSpaceConnectorName, AddressSpaceSpecConnector> {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9]+");

    @Override
    public boolean isValid(AddressSpaceSpecConnector connector, ConstraintValidatorContext context) {

        if (connector == null) {
            // if the object to check is null, we ignore it
            return true;
        }

        if (connector.getName() == null) {
            // we do require a name
            return false;
        }

        return NAME_PATTERN.matcher(connector.getName()).matches();
    }
}

