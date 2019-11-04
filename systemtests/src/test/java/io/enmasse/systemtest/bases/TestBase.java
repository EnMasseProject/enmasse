/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.listener.JunitCallbackListener;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import java.io.File;

/**
 * Base class for all tests
 */
@ExtendWith(JunitCallbackListener.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestBase implements ITestBase, ITestSeparator {
    private static Logger LOGGER = CustomLogger.getLogger();
    /**
     * The constant clusterUser.
     */
    protected static final UserCredentials clusterUser = new UserCredentials(KubeCMDClient.getOCUser());
    /**
     * The constant environment.
     */
    protected static final Environment environment = Environment.getInstance();
    /**
     * The constant logCollector.
     */
    protected static final GlobalLogCollector logCollector = new GlobalLogCollector(KUBERNETES,
            new File(environment.testLogDir()));
    /**
     * The Resources manager.
     */
    protected ResourceManager resourcesManager;
    /**
     * The Default credentials.
     */
    protected UserCredentials defaultCredentials = null;

    /**
     * Init test.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void initTest() throws Exception {
        LOGGER.info("Test init");
        resourcesManager = getResourceManager();
        if (TestInfo.getInstance().isTestShared()) {
            defaultCredentials = environment.getSharedDefaultCredentials();
            resourcesManager.setAddressSpacePlan(getDefaultAddressSpacePlan());
            resourcesManager.setAddressSpaceType(getAddressSpaceType().toString());
            resourcesManager.setDefaultAddSpaceIdentifier(getDefaultAddrSpaceIdentifier());
            if (resourcesManager.getSharedAddressSpace() == null) {
                resourcesManager.setup();
            }
        } else {
            defaultCredentials = environment.getDefaultCredentials();
            resourcesManager.setup();
        }
    }
}
