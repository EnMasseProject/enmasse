/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import io.enmasse.address.model.AddressSpaceSpecConnectorAddressRule;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class AddressSpaceConnectorAddressRulePatternValidator implements ConstraintValidator<AddressSpaceConnectorAddressRulePattern, AddressSpaceSpecConnectorAddressRule> {

    private static final Pattern PATTERN = Pattern.compile("([^/]+(/?[^/]+)*)*");

    @Override
    public boolean isValid(AddressSpaceSpecConnectorAddressRule rule, ConstraintValidatorContext context) {

        if (rule == null) {
            return true;
        }

        if (rule.getPattern() == null) {
            return false;
        }

        return PATTERN.matcher(rule.getPattern()).matches();
    }
}

