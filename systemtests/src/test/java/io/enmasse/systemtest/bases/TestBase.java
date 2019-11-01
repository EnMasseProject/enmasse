/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases;

import com.google.common.collect.Ordering;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.broker.BrokerManagement;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageType;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageType;
import io.enmasse.systemtest.listener.JunitCallbackListener;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientConnector;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.selenium.SeleniumManagement;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for all tests
 */
@ExtendWith(JunitCallbackListener.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestBase implements ITestBase, ITestSeparator {
    protected static Logger LOGGER = CustomLogger.getLogger();
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
    private UserCredentials managementCredentials = null;

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
            managementCredentials = environment.getSharedManagementCredentials();
            resourcesManager.setAddressSpacePlan(getDefaultAddressSpacePlan());
            resourcesManager.setAddressSpaceType(getAddressSpaceType().toString());
            resourcesManager.setDefaultAddSpaceIdentifier(getDefaultAddrSpaceIdentifier());
            if (resourcesManager.getSharedAddressSpace() == null) {
                resourcesManager.setup();
            }
        } else {
            defaultCredentials = environment.getDefaultCredentials();
            managementCredentials = environment.getManagementCredentials();
            resourcesManager.setup();
        }
    }
}
