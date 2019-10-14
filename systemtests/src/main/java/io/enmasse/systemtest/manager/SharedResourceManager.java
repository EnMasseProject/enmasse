/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.manager;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SharedResourceManager extends ResourceManager {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private static SharedResourceManager instance;
    private static Map<String, Integer> spaceCountMap = new HashMap<>();
    protected AmqpClientFactory amqpClientFactory = null;
    protected MqttClientFactory mqttClientFactory = null;
    private AddressSpace sharedAddressSpace = null;
    private static final String DEFAULT_ADDRESS_TEMPLATE = "-shared-";
    private UserCredentials defaultCredentials = environment.getSharedDefaultCredentials();
    private UserCredentials managementCredentials = environment.getSharedManagementCredentials();

    public static synchronized SharedResourceManager getInstance() {
        if (instance == null) {
            instance = new SharedResourceManager();
        }
        return instance;
    }

    @Override
    public AddressSpace getSharedAddressSpace() {
        return sharedAddressSpace;
    }

    public void setSharedAddressSpace(AddressSpace sharedAddressSpace) {
        this.sharedAddressSpace = sharedAddressSpace;
    }

    public void initFactories(UserCredentials userCredentials) {
        amqpClientFactory = new AmqpClientFactory(sharedAddressSpace, userCredentials);
        mqttClientFactory = new MqttClientFactory(sharedAddressSpace, userCredentials);
    }

    @Override
    public void tearDown(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            if (!environment.skipCleanup()) {
                LOGGER.info(String.format("test failed: %s.%s",
                        context.getTestClass().get().getName(),
                        context.getTestMethod().get().getName()));
                LOGGER.info("shared address space '{}' will be removed", sharedAddressSpace);
                try {
                    if (sharedAddressSpace != null) {
                        super.deleteAddressSpace(sharedAddressSpace);
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Failed to delete shared address space (ignored)", ex);
                } finally {
                    spaceCountMap.compute(defaultAddSpaceIdentifier, (k, count) -> count == null ? null : count + 1);
                }
            } else {
                LOGGER.warn("No address space is deleted, SKIP_CLEANUP is set");
            }
        } else { //succeed
            try {
                if (!environment.skipCleanup()) {
                    LOGGER.info("Shared address space will be deleted!");
                    super.deleteAddressSpace(sharedAddressSpace);
                } else {
                    LOGGER.warn("No address space is deleted, SKIP_CLEANUP is set");
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to delete addresses from shared address space (ignored)", e);
            }
        }
        closeClientFactories(amqpClientFactory, mqttClientFactory);
        amqpClientFactory = null;
        mqttClientFactory = null;
        sharedAddressSpace = null;
    }

    void initFactories(AddressSpace addressSpace) {
        amqpClientFactory = new AmqpClientFactory(sharedAddressSpace, defaultCredentials);
        mqttClientFactory = new MqttClientFactory(sharedAddressSpace, defaultCredentials);
    }

    @Override
    public void setup() throws Exception {
        LOGGER.info("Shared setup");
        if (spaceCountMap == null) {
            spaceCountMap = new HashMap<>();
        }
        spaceCountMap.putIfAbsent(defaultAddSpaceIdentifier, 0);
        sharedAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(defaultAddSpaceIdentifier + DEFAULT_ADDRESS_TEMPLATE + spaceCountMap.get(defaultAddSpaceIdentifier))
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(addressSpaceType)
                .withPlan(addressSpacePlan)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        createSharedAddressSpace(sharedAddressSpace);
        createOrUpdateUser(sharedAddressSpace, managementCredentials);
        createOrUpdateUser(sharedAddressSpace, defaultCredentials);
        initFactories(sharedAddressSpace);
    }

    public void createSharedAddressSpace(AddressSpace addressSpace) throws Exception {
        super.createAddressSpace(addressSpace);
        sharedAddressSpace = addressSpace;
    }

    @Override
    public void createAddressSpace(AddressSpace addressSpace) throws Exception {
        super.createAddressSpace(addressSpace);
    }

    public void deleteSharedAddressSpace() {
        if (sharedAddressSpace != null && TestInfo.getInstance().isAddressSpaceDeleteable()) {
            LOGGER.info("Shared address {} space will be removed", sharedAddressSpace.getMetadata().getName());
            Environment env = Environment.getInstance();
            if (!env.skipCleanup()) {
                Kubernetes kube = Kubernetes.getInstance();
                GlobalLogCollector logCollector = new GlobalLogCollector(kube, new File(env.testLogDir()));
                try {
                    AddressSpaceUtils.deleteAddressSpaceAndWait(sharedAddressSpace, logCollector);
                    sharedAddressSpace = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                LOGGER.warn("Remove address spaces when test run finished - SKIPPED!");
            }
        } else {
            if (sharedAddressSpace != null) {
                LOGGER.info("Shared address {} space will be reused", sharedAddressSpace.getMetadata().getName());
            }
        }
    }

    @Override
    public AmqpClientFactory getAmqpClientFactory() {
        return amqpClientFactory;
    }

    @Override
    public void setAmqpClientFactory(AmqpClientFactory amqpClientFactory) {
        this.amqpClientFactory = amqpClientFactory;
    }

    @Override
    public MqttClientFactory getMqttClientFactory() {
        return mqttClientFactory;
    }

    @Override
    public void setMqttClientFactory(MqttClientFactory mqttClientFactory) {
        this.mqttClientFactory = mqttClientFactory;
    }

}
