/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.enmasse.common.model.CustomResourceWithAdditionalProperties;

public class HasMetadataValidator implements ConstraintValidator<HasMetadata, CustomResourceWithAdditionalProperties<?, ?>> {

    private boolean needsNamespace;
    private boolean needsName;

    @Override
    public void initialize(HasMetadata constraintAnnotation) {
        this.needsNamespace = constraintAnnotation.needsNamespace();
        this.needsName = constraintAnnotation.needsName();
    }

    @Override
    public boolean isValid(final CustomResourceWithAdditionalProperties<?, ?> value, final ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        if (value.getMetadata() == null) {
            return false;
        }

        if (this.needsNamespace) {
            if (value.getMetadata().getNamespace() == null) {
                return false;
            }
        }

        if (this.needsName) {
            if (value.getMetadata().getName() == null) {
                return false;
            }
        }

        return true;

    }
}

