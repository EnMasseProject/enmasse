/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.condition;

import io.enmasse.systemtest.EnmasseInstallType;
import io.enmasse.systemtest.OLMInstallationType;

import java.lang.annotation.Annotation;

import static io.enmasse.systemtest.EnmasseInstallType.BUNDLE;
import static io.enmasse.systemtest.EnmasseInstallType.OLM;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SupportedInstallTypesConditionTest {

    public static void main(String[] args) {
        new SupportedInstallTypesConditionTest().testSupport();
    }

    void testSupport() {
        assertTrue(test(false, BUNDLE, BUNDLE));
        assertFalse(test(false, BUNDLE, OLM));
        assertFalse(test(false, OLM, BUNDLE));
        assertFalse(test(false, OLM, OLM));


        assertTrue(test(true, BUNDLE, BUNDLE));
        assertTrue(test(true, BUNDLE, OLM));
        assertFalse(test(true, OLM, BUNDLE));
        assertTrue(test(true, OLM, OLM));

    }


    private boolean test(boolean olmAvailable, EnmasseInstallType envInstallType, EnmasseInstallType supportedInstalls) {
        SupportedInstallType supports = supportsInstance(supportedInstalls);
        return new SupportedInstallTypeCondition().isTestEnabled(supports, olmAvailable, envInstallType);
    }

    private SupportedInstallType supportsInstance(EnmasseInstallType value) {
        return new SupportedInstallType() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return SupportedInstallType.class;
            }

            @Override
            public EnmasseInstallType value() {
                return value;
            }

            @Override
            public OLMInstallationType olmInstallType() {
                // TODO Auto-generated method stub
                return null;
            }

        };
    }

}
