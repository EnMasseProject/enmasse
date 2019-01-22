/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {KubeMetadataNameValidator.class})
@Documented
/**
 * Validate if metadata.name is a valid Kubernetes name
 */
public @interface KubeMetadataName {

    String message() default "Invalid Kubernetes metadata name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
