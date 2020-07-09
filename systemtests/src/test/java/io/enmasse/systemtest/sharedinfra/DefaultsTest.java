/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingInfrastructure;
import io.enmasse.api.model.MessagingInfrastructureCondition;
import io.enmasse.api.model.MessagingProject;
import io.enmasse.api.model.MessagingProjectCondition;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingProject;
import io.enmasse.systemtest.messaginginfra.resources.MessagingInfrastructureResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingProjectResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultsTest extends TestBase {

    @Test
    @DefaultMessagingInfrastructure
    public void testDefaultInfra(ExtensionContext extensionContext) {
        MessagingInfrastructure infra = resourceManager.getDefaultInfra();

        resourceManager.waitResourceCondition(infra, i -> {
            MessagingInfrastructureCondition condition = MessagingInfrastructureResourceType.getCondition(i.getStatus().getConditions(), "Ready");
            return condition != null && "True".equals(condition.getStatus());
        });


        assertEquals(1, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "router")).size());
        assertEquals(1, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "broker")).size());
    }

    @Test
    @DefaultMessagingInfrastructure
    @DefaultMessagingProject
    public void testDefaultProject(ExtensionContext extensionContext) {
        MessagingInfrastructure infra = resourceManager.getDefaultInfra();
        MessagingProject project = resourceManager.getDefaultMessagingProject();

        assertNotNull(project);
        resourceManager.waitResourceCondition(project, t -> {
            MessagingProjectCondition condition = MessagingProjectResourceType.getCondition(t.getStatus().getConditions(), "Ready");
            return condition != null && "True".equals(condition.getStatus());
        });
        assertEquals(infra.getMetadata().getName(), project.getStatus().getMessagingInfrastructureRef().getName());
        assertEquals(infra.getMetadata().getNamespace(), project.getStatus().getMessagingInfrastructureRef().getNamespace());
    }
}
