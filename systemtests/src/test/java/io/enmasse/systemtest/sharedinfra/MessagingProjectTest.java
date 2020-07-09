/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingInfrastructure;
import io.enmasse.api.model.MessagingInfrastructureBuilder;
import io.enmasse.api.model.MessagingProject;
import io.enmasse.api.model.MessagingProjectBuilder;
import io.enmasse.api.model.MessagingProjectCondition;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.messaginginfra.resources.MessagingProjectResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessagingProjectTest extends TestBase {

    @Test
    @DefaultMessagingInfrastructure
    public void testMultipleMessagingProjects(ExtensionContext extensionContext) {
        MessagingProject t1 = new MessagingProjectBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace("app1")
                .endMetadata()
                .build();

        MessagingProject t2 = new MessagingProjectBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace("app2")
                .endMetadata()
                .build();

        resourceManager.createResource(extensionContext, t1, t2);

        t1 = MessagingProjectResourceType.getOperation().inNamespace(t1.getMetadata().getNamespace()).withName(t1.getMetadata().getName()).get();
        assertNotNull(t1);
        MessagingProjectCondition condition = MessagingProjectResourceType.getCondition(t1.getStatus().getConditions(), "Ready");
        assertNotNull(condition);
        assertEquals("True", condition.getStatus());
        assertEquals("default-infra", t1.getStatus().getMessagingInfrastructureRef().getName());
        assertEquals(environment.namespace(), t1.getStatus().getMessagingInfrastructureRef().getNamespace());

        t2 = MessagingProjectResourceType.getOperation().inNamespace(t2.getMetadata().getNamespace()).withName(t2.getMetadata().getName()).get();
        assertNotNull(t2);
        condition = MessagingProjectResourceType.getCondition(t2.getStatus().getConditions(), "Ready");
        assertNotNull(condition);
        assertEquals("True", condition.getStatus());
        assertEquals("default-infra", t2.getStatus().getMessagingInfrastructureRef().getName());
        assertEquals(environment.namespace(), t2.getStatus().getMessagingInfrastructureRef().getNamespace());
    }

    @Test
    public void testSelectors(ExtensionContext extensionContext) {
        MessagingInfrastructure infra = new MessagingInfrastructureBuilder()
                .withNewMetadata()
                .withName("default-infra")
                .withNamespace(environment.namespace())
                .endMetadata()
                .withNewSpec()
                .editOrNewNamespaceSelector()
                .addNewMatchName("app1")
                .endNamespaceSelector()
                .endSpec()
                .build();

        MessagingProject t1 = new MessagingProjectBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace("app1")
                .endMetadata()
                .build();

        MessagingProject t2 = new MessagingProjectBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace("app2")
                .endMetadata()
                .build();

        resourceManager.createResource(extensionContext, infra, t1);
        resourceManager.createResource(extensionContext, false, t2);

        assertTrue(resourceManager.waitResourceCondition(t2, messagingProject ->
                messagingProject != null &&
                        messagingProject.getStatus() != null &&
                        MessagingProjectResourceType.getCondition(messagingProject.getStatus().getConditions(), "Bound") != null &&
                        Objects.requireNonNull(MessagingProjectResourceType.getCondition(messagingProject.getStatus().getConditions(), "Bound")).getStatus() != null &&
                        Objects.requireNonNull(MessagingProjectResourceType.getCondition(messagingProject.getStatus().getConditions(), "Bound")).getStatus().equals("False")));
    }
}
