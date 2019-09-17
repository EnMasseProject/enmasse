/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import io.enmasse.address.model.AddressSpaceSpecConnectorAddressRule;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class AddressSpaceConnectorAddressRuleNameValidator implements ConstraintValidator<AddressSpaceConnectorAddressRuleName, AddressSpaceSpecConnectorAddressRule> {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9]+");

    @Override
    public boolean isValid(AddressSpaceSpecConnectorAddressRule rule, ConstraintValidatorContext context) {

        if (rule == null) {
            return true;
        }

        if (rule.getName() == null) {
            return false;
        }

        return NAME_PATTERN.matcher(rule.getName()).matches();
    }
}

