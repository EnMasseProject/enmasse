/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Comparator;

public class KubeResourceApplier {
    private static final Logger log = LoggerFactory.getLogger(KubeResourceApplier.class);
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    /**
     * Apply new version of resources if considered changed
     */
    public static <T extends HasMetadata, L extends KubernetesResourceList<T>, D extends Doneable<T>>
        void applyIfDifferent(File resourceDir,
                          NonNamespaceOperation<T, L, D, Resource<T, D>> operation,
                          Class<T> resourceClass,
                          Comparator<T> comparator) {
        if (resourceDir.isDirectory()) {
            File[] files = resourceDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    T newResource = readAndParse(file, resourceClass);
                    if (newResource != null) {
                        T currentResource = operation.withName(newResource.getMetadata().getName()).get();
                        if (currentResource == null) {
                            log.info("Creating {} {}", newResource.getKind(), newResource.getMetadata().getName());
                            operation.create(newResource);
                        } else if (comparator.compare(currentResource, newResource) != 0) {
                            log.info("Updating {} {}", newResource.getKind(), newResource.getMetadata().getName());
                            operation.createOrReplace(newResource);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates resources in resourceDir if no resource of this type exists.
     */
    public static <T extends HasMetadata, L extends KubernetesResourceList<T>, D extends Doneable<T>>
    void createIfNoneExists(File resourceDir,
                          NonNamespaceOperation<T, L, D, Resource<T, D>> operation,
                          Class<T> resourceClass) {
        if (operation.list().getItems().isEmpty()) {
            if (resourceDir.isDirectory()) {
                File[] files = resourceDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        T newResource = readAndParse(file, resourceClass);
                        log.info("Creating {} {}", newResource.getKind(), newResource.getMetadata().getName());
                        operation.create(newResource);
                    }
                }
            }
        }
    }

    private static <T> T readAndParse(File file, Class<T> resourceClass) {
        try {
            return mapper.readValue(file, resourceClass);
        } catch (Exception e) {
            log.error("Error reading and parsing file {} (skipped): {}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }
}
