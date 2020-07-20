/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.messaginginfra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.enmasse.systemtest.framework.ThrowableRunner;
import io.enmasse.systemtest.messaginginfra.resources.MessagingAddressResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingInfrastructureResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingProjectResourceType;
import io.enmasse.systemtest.messaginginfra.resources.NamespaceResourceType;
import io.enmasse.systemtest.messaginginfra.resources.ResourceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Managing resources
 */
public class ResourceManager {

    private static final Logger LOGGER = LoggerUtils.getLogger();
    private static boolean verbose = true;

    private static final Map<String, Stack<ThrowableRunner>> storedResources = new LinkedHashMap<>();
    private static final Map<String, Map<String, AtomicLong>> defaultResources = new HashMap<>();

    private final Kubernetes kubeClient = Kubernetes.getInstance();
    private AmqpClientFactory amqpClientFactory = new AmqpClientFactory(new UserCredentials("dummy", "dummy"));

    private static ResourceManager instance;

    public static synchronized ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    public void deleteResources(ExtensionContext testContext) throws Exception {
        LoggerUtils.logDelimiter("-");
        LOGGER.info("Going to clear all resources for {}", testContext.getDisplayName());
        LoggerUtils.logDelimiter("-");
        if (!storedResources.containsKey(testContext.getDisplayName()) || storedResources.get(testContext.getDisplayName()).isEmpty()) {
            LOGGER.info("Nothing to delete");
        }
        while (storedResources.containsKey(testContext.getDisplayName()) && !storedResources.get(testContext.getDisplayName()).isEmpty()) {
            storedResources.get(testContext.getDisplayName()).pop().run();
        }
        LoggerUtils.logDelimiter("-");
        LOGGER.info("");
        storedResources.remove(testContext.getDisplayName());
    }

    public void enableVerboseLogging() {
        verbose = true;
    }

    public void disableVerboseLogging() {
        verbose = false;
    }

    //------------------------------------------------------------------------------------------------
    // Default resources methods
    //------------------------------------------------------------------------------------------------
    public synchronized void addDefaultResource(HasMetadata resource) {
        defaultResources.computeIfAbsent(resource.getKind(), k -> new HashMap<>());
        defaultResources.get(resource.getKind()).putIfAbsent(buildIdentifier(resource), new AtomicLong(0));
        defaultResources.get(resource.getKind()).get(buildIdentifier(resource)).incrementAndGet();
        LOGGER.info("Default resource of kind {} with name {}, usage {}", resource.getKind(), resource.getMetadata().getName(), defaultResources.get(resource.getKind()).get(buildIdentifier(resource)));
    }

    @SuppressWarnings(value = "unchecked")
    public synchronized <T extends HasMetadata> T getDefaultResource(String kind) {
        String identifier = defaultResources.get(kind).entrySet().iterator().next().getKey();
        return (T) Objects.requireNonNull(findResourceType(kind)).get(getNamespaceFromIdentifier(identifier), getNameFromIdentifier(identifier));
    }

    public synchronized void removeDefaultResource(HasMetadata resource) {
        Optional.ofNullable(defaultResources.get(resource.getKind())).ifPresent(m -> m.remove(resource.getKind()));
    }

    //------------------------------------------------------------------------------------------------
    // Client factories
    //------------------------------------------------------------------------------------------------


    public void closeAmqpFactory() throws Exception {
        if (amqpClientFactory != null) {
            amqpClientFactory.close();
            amqpClientFactory = null;
        }
    }

    public void closeFactories() throws Exception {
        closeAmqpFactory();
    }

    public AmqpClientFactory getAmqpClientFactory() {
        return amqpClientFactory;
    }

    //------------------------------------------------------------------------------------------------
    // Common resource methods
    //------------------------------------------------------------------------------------------------

    private final ResourceType<?>[] resourceTypes = new ResourceType[]{
            new MessagingInfrastructureResourceType(),
            new MessagingProjectResourceType(),
            new MessagingAddressResourceType(),
            new MessagingEndpointResourceType(),
            new NamespaceResourceType(),
    };

    @SafeVarargs
    public final <T extends HasMetadata> void createResource(ExtensionContext testContext, T... resources) {
        createResource(testContext, true, resources);
    }

    @SafeVarargs
    public final <T extends HasMetadata> void createResource(ExtensionContext testContext, boolean waitReady, T... resources) {
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            if (type == null) {
                LOGGER.warn("Can't find resource in list, please create it manually");
                continue;
            }

            // Convenience for tests that create resources in non-existing namespaces. This will create and clean them up.
            synchronized (this) {
                if (resource.getMetadata().getNamespace() != null && !kubeClient.namespaceExists(resource.getMetadata().getNamespace())) {
                    createResource(testContext, waitReady, new NamespaceBuilder().editOrNewMetadata().withName(resource.getMetadata().getNamespace()).endMetadata().build());
                }
            }

            if (verbose) {
                LOGGER.info("Create/Update of {} {} in namespace {}",
                        resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace() == null ? "(not set)" : resource.getMetadata().getNamespace());
            }

            type.create(resource);

            synchronized (this) {
                storedResources.computeIfAbsent(testContext.getDisplayName(), k -> new Stack<>());
                storedResources.get(testContext.getDisplayName()).push(() -> deleteResource(resource));
            }
        }

        if (waitReady) {
            for (T resource : resources) {
                ResourceType<T> type = findResourceType(resource);
                if (type == null) {
                    LOGGER.warn("Can't find resource in list, please create it manually");
                    continue;
                }

                assertTrue(waitResourceCondition(resource, type::isReady),
                        String.format("Timed out waiting for %s %s in namespace %s to be ready", resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace()));

                T updated = type.get(resource.getMetadata().getNamespace(), resource.getMetadata().getName());
                type.refreshResource(resource, updated);
            }
        }
    }

    @SafeVarargs
    public final <T extends HasMetadata> void deleteResource(T... resources) throws Exception {
        for (T resource : resources) {
            boolean deletable = true;
            ResourceType<T> type = findResourceType(resource);
            if (type == null) {
                LOGGER.warn("Can't find resource type, please delete it manually");
                continue;
            }
            if (verbose) {
                LOGGER.info("Delete of {} {} in namespace {}",
                        resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace() == null ? "(not set)" : resource.getMetadata().getNamespace());
            }
            if (isDefaultResource(resource)) {
                defaultResources.get(resource.getKind()).get(buildIdentifier(resource)).decrementAndGet();
                LOGGER.info("Default resource of kind {} with name {}, usage {}",
                        resource.getKind(), resource.getMetadata().getName(),
                        defaultResources.get(resource.getKind()).get(buildIdentifier(resource)));
                deletable = isDefaultResourceDeletable(resource);
            }
            if (deletable) {
                type.delete(resource);
                assertTrue(waitResourceCondition(resource, Objects::isNull),
                        String.format("Timed out deleting %s %s in namespace %s", resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace()));
            }
        }
    }

    public final <T extends HasMetadata> boolean waitResourceCondition(T resource, Predicate<T> condition) {
        return waitResourceCondition(resource, condition, TimeoutBudget.ofDuration(Duration.ofMinutes(5)));
    }

    public final <T extends HasMetadata> boolean waitResourceCondition(T resource, Predicate<T> condition, TimeoutBudget timeout) {
        assertNotNull(resource);
        assertNotNull(resource.getMetadata());
        assertNotNull(resource.getMetadata().getName());
        ResourceType<T> type = findResourceType(resource);
        assertNotNull(type);

        while (!timeout.timeoutExpired()) {
            T res = type.get(resource.getMetadata().getNamespace(), resource.getMetadata().getName());
            if (condition.test(res)) {
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        T res = type.get(resource.getMetadata().getNamespace(), resource.getMetadata().getName());
        boolean pass = condition.test(res);
        if (!pass) {
            LOGGER.info("Resource failed condition check: {}", resourceToString(res));
        }
        return pass;
    }

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static <T extends HasMetadata> String resourceToString(T resource) {
        if (resource == null) {
            return "null";
        }
        try {
            return mapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            LOGGER.info("Failed converting resource to YAML: {}", e.getMessage());
            return "unknown";
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

    @SuppressWarnings(value = "unchecked")
    private <T extends HasMetadata> ResourceType<T> findResourceType(String kind) {
        for (ResourceType<?> type : resourceTypes) {
            if (type.getKind().equals(kind)) {
                return (ResourceType<T>) type;
            }
        }
        return null;
    }

    private String buildIdentifier(HasMetadata resource) {
        return String.format("%s__%s", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
    }

    private String getNamespaceFromIdentifier(String id) {
        return id.split("__")[1];
    }

    private String getNameFromIdentifier(String id) {
        return id.split("__")[0];
    }

    private boolean isDefaultResource(HasMetadata resource) {
        return defaultResources.containsKey(resource.getKind()) &&
                defaultResources.get(resource.getKind()).containsKey(buildIdentifier(resource));
    }

    private boolean isDefaultResourceDeletable(HasMetadata resource) {
        if (defaultResources.get(resource.getKind()).get(buildIdentifier(resource)).get() > 0) {
            LOGGER.info("Default resource of kind {}, with name {} still in use deletion skipped", resource.getKind(), resource.getMetadata().getName());
            return false;
        }
        return true;
    }
}
