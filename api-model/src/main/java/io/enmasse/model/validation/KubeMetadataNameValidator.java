/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.enmasse.address.model.KubeUtil;
import io.enmasse.common.model.AbstractHasMetadata;

public class KubeMetadataNameValidator implements ConstraintValidator<KubeMetadataName, AbstractHasMetadata<?>> {

    @Override
    public boolean isValid(AbstractHasMetadata<?> value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        if (value.getMetadata() == null) {
            return false;
        }

        if (value.getMetadata().getName() == null) {
            return false;
        }

        return KubeUtil.isNameValid(value.getMetadata().getName());

    }
}

