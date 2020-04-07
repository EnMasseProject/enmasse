/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingInfraBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ThrowableRunner;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.messaginginfra.crds.MessagingInfraCrd;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Managing resources //TODO rename it once enmasse will be moved to 1.0
 */
public class MessagingInfraResourceManager {

    private static final Logger LOGGER = CustomLogger.getLogger();

    private static Stack<ThrowableRunner> classResources = new Stack<>();
    private static Stack<ThrowableRunner> methodResources = new Stack<>();
    private static Stack<ThrowableRunner> pointerResources = classResources;
    private Kubernetes kubeClient = Kubernetes.getInstance();
    private final Environment environment = Environment.getInstance();
    private AmqpClientFactory amqpClientFactory = null;
    private MqttClientFactory mqttClientFactory = null;
    private final GlobalLogCollector logCollector = new GlobalLogCollector(kubeClient, environment.testLogDir());

    private static MessagingInfraResourceManager instance;

    public static synchronized MessagingInfraResourceManager getInstance() {
        if (instance == null) {
            instance = new MessagingInfraResourceManager();
        }
        return instance;
    }

    public Stack<ThrowableRunner> getPointerResources() {
        return pointerResources;
    }

    public void setMethodResources() {
        LOGGER.info("Setting pointer to method resources");
        pointerResources = methodResources;
    }

    public void setClassResources() {
        LOGGER.info("###################################");
        LOGGER.info("Setting pointer to class resources");
        pointerResources = classResources;
    }

    public void deleteClassResources() throws Exception {
        LOGGER.info("Going to clear all class resources");
        while (!classResources.empty()) {
            classResources.pop().run();
        }
        classResources.clear();
    }

    public void deleteMethodResources() throws Exception {
        LOGGER.info("Going to clear all method resources");
        while (!methodResources.empty()) {
            methodResources.pop().run();
        }
        methodResources.clear();
        pointerResources = classResources;
    }

    //------------------------------------------------------------------------------------------------
    // Client factories
    //------------------------------------------------------------------------------------------------

    public void initFactories(AddressSpace addressSpace) {
        amqpClientFactory = new AmqpClientFactory(addressSpace, environment.getSharedDefaultCredentials());
        mqttClientFactory = new MqttClientFactory(addressSpace, environment.getSharedDefaultCredentials());
    }

    public void initFactories(AddressSpace addressSpace, UserCredentials cred) {
        amqpClientFactory = new AmqpClientFactory(addressSpace, cred);
        mqttClientFactory = new MqttClientFactory(addressSpace, cred);
    }

    public void initFactories(IoTProject project, UserCredentials credentials) {
        String addSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
        initFactories(kubeClient.getAddressSpaceClient(project.getMetadata()
                .getNamespace()).withName(addSpaceName).get(), credentials);
    }

    public void closeAmqpFactory() throws Exception {
        if (amqpClientFactory != null) {
            amqpClientFactory.close();
            amqpClientFactory = null;
        }
    }

    public void closeMqttFactory() {
        if (mqttClientFactory != null) {
            mqttClientFactory.close();
            mqttClientFactory = null;
        }
    }

    public void closeFactories() throws Exception {
        closeAmqpFactory();
        closeMqttFactory();
    }

    public AmqpClientFactory getAmqpClientFactory() {
        return amqpClientFactory;
    }

    public MqttClientFactory getMqttClientFactory() {
        return mqttClientFactory;
    }

    //------------------------------------------------------------------------------------------------
    // Common resource methods
    //------------------------------------------------------------------------------------------------

    public <T extends HasMetadata> void createResource(T... resources) {
        for (T resource : resources) {
            LOGGER.info("Create/Update of {} {} in namespace {}",
                    resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace() == null ? "(not set)" : resource.getMetadata().getNamespace());
            switch (resource.getKind()) {
                case MessagingInfraCrd.KIND:
                    createMessagingInfra((MessagingInfra) resource);
                    scheduleDeletion(MessagingInfraCrd.getClient(), (MessagingInfra) resource);
                    waitForInfraUp((MessagingInfra) resource);
                    break;
                default:
                    LOGGER.warn("Can't find resource in list, please create it manually");
                    break;
            }
        }
    }

    public <T extends HasMetadata> void removeResource(T... resources) throws Exception {
        for (T resource : resources) {
            LOGGER.info("Delete of {} {} in namespace {}",
                    resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace() == null ? "(not set)" : resource.getMetadata().getNamespace());
            switch (resource.getKind()) {
                case "MessagingInfra":
                    deleteMessagingInfra((MessagingInfra) resource);
                    break;
                default:
                    LOGGER.warn("Can't find resource in list, please delete it manually");
                    break;
            }
        }
    }

    public <T extends HasMetadata> T scheduleDeletion(MixedOperation<T, ?, ?, ?> operation, T resource) {
        LOGGER.info("Scheduled deletion of {} {} in namespace {}",
                resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace() == null ? "(not set)" : resource.getMetadata().getNamespace());
        switch (resource.getKind()) {
            case "MessagingInfra":
                pointerResources.push(() ->
                        deleteMessagingInfra((MessagingInfra) resource));
                break;
            default:
                pointerResources.push(() -> {
                    LOGGER.info("Deleting {} {} in namespace {}",
                            resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                    operation.inNamespace(resource.getMetadata().getNamespace())
                            .withName(resource.getMetadata().getName())
                            .cascading(true)
                            .delete();
                });
        }
        return resource;
    }

    //*********************************************************************************************
    // Messaging Infra
    //*********************************************************************************************
    private void createMessagingInfra(MessagingInfra resource) {
        MessagingInfraCrd.getClient().inNamespace(resource.getMetadata().getNamespace()).createOrReplace(new MessagingInfraBuilder(resource)
                .editOrNewMetadata()
                .withNewResourceVersion("")
                .endMetadata()
                .withNewStatus()
                .endStatus()
                .build());
    }

    private void deleteMessagingInfra(MessagingInfra resource) {
        if (MessagingInfraCrd.getClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).get() != null) {
            LOGGER.info("Delete of {} {} in namespace {}",
                    resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace() == null ? "(not set)" : resource.getMetadata().getNamespace());
            MessagingInfraCrd.getClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).delete();
        }
    }

    public void waitForInfraUp(MessagingInfra infra) {
        MessagingInfra found = null;
        TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));
        while (!budget.timeoutExpired()) {
            found = MessagingInfraCrd.getClient().inNamespace(infra.getMetadata().getNamespace()).withName(infra.getMetadata().getName()).get();
            assertNotNull(found);
            if (found.getStatus() != null &&
                    "Active".equals(found.getStatus().getPhase())) {
                break;
            }
        }
        assertNotNull(found);
        assertNotNull(found.getStatus());
        assertEquals("Active", found.getStatus().getPhase());
        infra.setMetadata(found.getMetadata());
        infra.setSpec(found.getSpec());
        infra.setStatus(found.getStatus());
    }
}
