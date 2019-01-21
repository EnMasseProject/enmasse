/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.KubeUtil;

public class AddressNameValidator implements ConstraintValidator<AddressName, Address> {

    @SuppressWarnings("deprecation")
    @Override
    public boolean isValid(Address address, ConstraintValidatorContext context) {

        if (address == null) {
            return true;
        }

        if (address.getMetadata() == null || address.getMetadata().getName() == null) {
            return false;
        }

        final String name = address.getMetadata().getName();

        String[] components = name.split("\\.");
        if (components.length < 2) {
            context.disableDefaultConstraintViolation();
            context
                    .buildConstraintViolationWithTemplate("Address name must be on the form addressSpace.addressName")
                    .addConstraintViolation();
            return false;
        }
        if ( address.getSpec() != null && address.getSpec().getAddressSpace() != null ) {
            // we only validate the address space name if it is set, as this is an optional legacy value
            if (!components[0].equals(address.getSpec().getAddressSpace())) {
                context.disableDefaultConstraintViolation();
                context
                        .buildConstraintViolationWithTemplate("Address space component of address name does not match address space")
                        .addConstraintViolation();
                return false;
            }
        }

        for (String component : components) {
            if (!KubeUtil.isNameValid(component)) {
                return false;
            }
        }

        return true;
    }
}

