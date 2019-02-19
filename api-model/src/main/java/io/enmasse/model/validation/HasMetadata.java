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
@Constraint(validatedBy = {HasMetadataValidator.class})
@Documented
/**
 * Validate if the object has metadata set.
*/
public @interface HasMetadata {

    String message() default "Metadata must be present";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Does the metadata need to have a name set.
     */
    boolean needsName() default true;
    /**
     * Does the metadata need to have a namespace set.
     */
    boolean needsNamespace() default false;

}
