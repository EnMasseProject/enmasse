/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.isolated;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.api.model.DoneableMessagingInfra;
import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingInfraList;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.Tag;

import static io.enmasse.systemtest.TestTag.ISOLATED_SHARED_INFRA;

@Tag(ISOLATED_SHARED_INFRA)
public interface ITestIsolatedSharedInfra extends ITestBase {
    MixedOperation<MessagingInfra, MessagingInfraList, DoneableMessagingInfra, Resource<MessagingInfra, DoneableMessagingInfra>> messagingInfraClient = kubernetes.getClient().customResources(CoreCrd.messagingInfras(), MessagingInfra.class, MessagingInfraList.class, DoneableMessagingInfra.class);

    IsolatedResourcesManager isolatedResourcesManager = IsolatedResourcesManager.getInstance();

    default AmqpClientFactory getAmqpClientFactory() {
        return isolatedResourcesManager.getAmqpClientFactory();
    }

    default MqttClientFactory getMqttClientFactory() {
        return isolatedResourcesManager.getMqttClientFactory();
    }

    @Override
    default ResourceManager getResourceManager() {
        return isolatedResourcesManager;
    }
}
