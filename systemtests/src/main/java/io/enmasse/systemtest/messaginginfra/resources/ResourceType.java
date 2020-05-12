/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.MixedOperation;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNull;

public interface ResourceType<T extends HasMetadata> {
    String getKind();
    void create(T resource);
    void delete(T resource) throws Exception;
    void waitReady(T resource);

    default void waitDeleted(MixedOperation<T, ?, ?, ?> operation, T resource) throws InterruptedException {
        TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));
        while (!budget.timeoutExpired() && operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).get() != null) {
            Thread.sleep(500);
        }
        assertNull(operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).get());
    }
}
