/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.olm;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.enmasse.systemtest.EnmasseInstallType;
import io.enmasse.systemtest.OLMInstallationType;
import io.enmasse.systemtest.bases.olm.OLMTestBase;
import io.enmasse.systemtest.condition.SupportedInstallType;

//disabled temporarly because of https://github.com/EnMasseProject/enmasse/pull/3482
@Disabled
@Tag(ACCEPTANCE)
@SupportedInstallType(value = EnmasseInstallType.OLM, olmInstallType = OLMInstallationType.SPECIFIC)
public class OLMSpecificTest extends OLMTestBase{

    @Override
    protected String getInstallationNamespace() {
        return kubernetes.getInfraNamespace();
    }

    @Test
    void testExampleResources() throws Exception {
        doTestCreateExampleResources();
    }

}
