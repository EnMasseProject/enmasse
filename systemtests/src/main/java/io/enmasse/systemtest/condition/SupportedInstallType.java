/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.enmasse.systemtest.EnmasseInstallType;
import io.enmasse.systemtest.OLMInstallationType;

/**
 * The use of this annotation enables/disables the target test class depending if OLM is supported in the cluster where tests are run.
 *
 * Important note:
 * According to the current systemtests implementation, using this annotation will force the deployment of enmasse into the cluster,
 * so if enmasse is already deployed, regardless of the installation type, enmasse will be undeployed and redeployed with the specific
 * settings indicated in this annotation
 *
 * @author famartin
 *
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(SupportedInstallTypeCondition.class)
public @interface SupportedInstallType {

    EnmasseInstallType value();

    OLMInstallationType olmInstallType() default OLMInstallationType.DEFAULT;

}