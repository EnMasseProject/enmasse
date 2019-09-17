/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {AddressSpaceConnectorAddressRulePatternValidator.class})
@Documented
/**
 * Validate the pattern field of an address space connector.
 */
public @interface AddressSpaceConnectorAddressRulePattern {

    String message() default "Invalid address space connector address rule pattern";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
