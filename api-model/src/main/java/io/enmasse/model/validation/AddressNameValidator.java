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
    public boolean isValid(final Address address, final ConstraintValidatorContext context) {

        if (address == null) {
            // if the object to check is null, we ignore it
            return true;
        }

        if (address.getMetadata() == null || address.getMetadata().getName() == null) {
            // we don't have a name set, and that is ok for this check
            return true;
        }

        // but if we have a name set, then it must validate ...

        final String name = address.getMetadata().getName();

        final String[] components = name.split("\\.");
        if (components.length < 2) {
            context.disableDefaultConstraintViolation();
            context
                    .buildConstraintViolationWithTemplate("Address name must be on the form addressSpace.addressName")
                    .addConstraintViolation();
            return false;
        }

        if (address.getSpec() != null && address.getSpec().getAddressSpace() != null) {
            // we only validate the address space name if it is set, as this is an optional legacy value
            if (!components[0].equals(address.getSpec().getAddressSpace())) {
                context.disableDefaultConstraintViolation();
                context
                        .buildConstraintViolationWithTemplate("Address space component of address name does not match address space")
                        .addConstraintViolation();
                return false;
            }
        }

        if (address.getSpec() != null && address.getSpec().getAddress() != null && address.getSpec().getAddress().startsWith("__")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Address string must not start with double underscores '__'")
                    .addConstraintViolation();
            return false;
        }

        // ensure that each name component is a valid kubernetes name component

        int i = 0;
        for (final String component : components) {
            if (!KubeUtil.isNameValid(component)) {
                context.disableDefaultConstraintViolation();
                context
                        .buildConstraintViolationWithTemplate("Address name component is invalid")
                        .addPropertyNode("metadata")
                        .addPropertyNode("name")
                        .inIterable().atIndex(i)
                        .addConstraintViolation();
                return false;
            }
            i++;
        }

        return true;
    }
}

