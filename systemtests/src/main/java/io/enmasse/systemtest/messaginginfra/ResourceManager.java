/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ThrowableRunner;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messaginginfra.resources.MessagingAddressResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingInfraResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingTenantResourceType;
import io.enmasse.systemtest.messaginginfra.resources.NamespaceResourceType;
import io.enmasse.systemtest.messaginginfra.resources.ResourceType;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import org.slf4j.Logger;

import java.util.Stack;

/**
 * Managing resources
 */
public class ResourceManager {

    private static final Logger LOGGER = CustomLogger.getLogger();
    private static boolean verbose = true;

    private static Stack<ThrowableRunner> classResources = new Stack<>();
    private static Stack<ThrowableRunner> methodResources = new Stack<>();
    private static Stack<ThrowableRunner> pointerResources = classResources;

    private MessagingInfra defaultInfra;
    private MessagingTenant defaultTenant;

    private Kubernetes kubeClient = Kubernetes.getInstance();
    private final Environment environment = Environment.getInstance();
    private AmqpClientFactory amqpClientFactory = new AmqpClientFactory(null, new UserCredentials("dummy", "dummy"));
    private MqttClientFactory mqttClientFactory = null;

    private static ResourceManager instance;

    public static synchronized ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
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
        LOGGER.info("Setting pointer to class resources");
        pointerResources = classResources;
    }

    public void deleteClassResources() throws Exception {
        LOGGER.info("Going to clear all class resources");
        LOGGER.info("------------------------------------");
        while (!classResources.empty()) {
            classResources.pop().run();
        }
        LOGGER.info("------------------------------------");
        classResources.clear();
    }

    public void deleteMethodResources() throws Exception {
        LOGGER.info("Going to clear all method resources");
        LOGGER.info("------------------------------------");
        while (!methodResources.empty()) {
            methodResources.pop().run();
        }
        LOGGER.info("------------------------------------");
        methodResources.clear();
        pointerResources = classResources;
    }

    private void cleanDefault(HasMetadata resource) {
        if (defaultInfra != null && defaultInfra.getKind().equals(resource.getKind()) && defaultInfra.getMetadata().getName().equals(resource.getMetadata().getName())) {
            defaultInfra = null;
        }
        if (defaultTenant != null && defaultTenant.getKind().equals(resource.getKind()) && defaultTenant.getMetadata().getName().equals(resource.getMetadata().getName())) {
            defaultTenant = null;
        }
    }

    public void enableVerboseLogging() {
        verbose = true;
    }

    public void disableVerboseLogging() {
        verbose = false;
    }

    //------------------------------------------------------------------------------------------------
    // Pointers to default resources
    //------------------------------------------------------------------------------------------------
    public MessagingInfra getDefaultInfra() {
        return defaultInfra;
    }

    public MessagingTenant getDefaultMessagingTenant() {
        return defaultTenant;
    }

    public void setDefaultInfra(MessagingInfra infra) {
        defaultInfra = infra;
    }

    public void setDefaultMessagingTenant(MessagingTenant tenant) {
        defaultTenant = tenant;
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

    private final ResourceType<?>[] resourceTypes = new ResourceType[]{
            new MessagingInfraResourceType(),
            new MessagingTenantResourceType(),
            new MessagingAddressResourceType(),
            new MessagingEndpointResourceType(),
            new NamespaceResourceType(),
    };

    @SafeVarargs
    public final <T extends HasMetadata> void createResource(T... resources) {
        createResource(true, resources);
    }

    @SafeVarargs
    public final <T extends HasMetadata> void createResource(boolean waitReady, T... resources) {

        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            if (type == null) {
                LOGGER.warn("Can't find resource in list, please create it manually");
                continue;
            }

            // Convenience for tests that create resources in non-existing namespaces. This will create and clean them up.
            if (resource.getMetadata().getNamespace() != null && !kubeClient.namespaceExists(resource.getMetadata().getNamespace())) {
                createResource(waitReady, new NamespaceBuilder().editOrNewMetadata().withName(resource.getMetadata().getNamespace()).endMetadata().build());
            }

            if (verbose) {
                LOGGER.info("Create/Update of {} {} in namespace {}",
                        resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace() == null ? "(not set)" : resource.getMetadata().getNamespace());
            } else {
                LOGGER.info("Delete {} resources", resources.length);
            }

            type.create(resource);
        }

        pointerResources.push(() -> {
            deleteResource(resources);
        });

        if (waitReady) {
            waitResourceReady(resources);
        }
    }

    @SafeVarargs
    public final <T extends HasMetadata> void deleteResource(T... resources) throws Exception {
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            if (type == null) {
                LOGGER.warn("Can't find resource type, please create it manually");
                continue;
            }
            if (verbose) {
                LOGGER.info("Delete of {} {} in namespace {}",
                        resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace() == null ? "(not set)" : resource.getMetadata().getNamespace());
            } else {
                LOGGER.info("Delete {} resources", resources.length);
            }
            type.delete(resource);
            cleanDefault(resource);
        }
    }

    @SafeVarargs
    public final <T extends HasMetadata> void waitResourceReady(T... resources) {
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            if (type == null) {
                LOGGER.warn("Can't find resource in list, please create it manually");
                continue;
            }
            type.waitReady(resource);
        }
    }

    @SuppressWarnings(value = "unchecked")
    private <T extends HasMetadata> ResourceType<T> findResourceType(T resource) {
        for (ResourceType<?> type : resourceTypes) {
            if (type.getKind().equals(resource.getKind())) {
                return (ResourceType<T>) type;
            }
        }
        return null;
    }
}
