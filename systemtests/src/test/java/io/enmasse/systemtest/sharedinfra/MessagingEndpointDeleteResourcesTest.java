/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingAddressCondition;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingEndpointCondition;
import io.enmasse.api.model.MessagingProject;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingProject;
import io.enmasse.systemtest.framework.annotations.ExternalClients;
import io.enmasse.systemtest.messaginginfra.resources.MessagingAddressResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AssertionUtils;
import io.enmasse.systemtest.utils.Conditions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.Duration;
import java.util.concurrent.locks.Condition;

import static io.enmasse.systemtest.utils.Conditions.condition;
import static io.enmasse.systemtest.utils.Conditions.gone;
import static io.enmasse.systemtest.utils.Conditions.not;
import static io.enmasse.systemtest.utils.TestUtils.waitUntilCondition;
import static io.enmasse.systemtest.utils.TestUtils.waitUntilConditionOrFail;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DefaultMessagingInfrastructure
@DefaultMessagingProject
@ExternalClients
public class MessagingEndpointDeleteResourcesTest extends TestBase {

    @Test
    public void testNodePortEndpoint(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-node")
                .endMetadata()
                .editOrNewSpec()
                .withHost(kubernetes.getHost())
                .addToProtocols("AMQP")
                .editOrNewNodePort()
                .endNodePort()
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue1", project.getMetadata().getNamespace(), endpoint);
        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP", endpoint), "queue1", false, false);
        AssertionUtils.assertDefaultMessaging(clientRunner);

        // the delete service

        var serviceAccess = Kubernetes.getClient()
                .services()
                .inNamespace(resourceManager.getDefaultInfra().getMetadata().getNamespace())
                .withName(endpoint.getMetadata().getNamespace() + "-" + endpoint.getMetadata().getName());
        var oldService = serviceAccess.get();
        serviceAccess.delete();

        // wait for the old service to disappear

        var uid = oldService.getMetadata().getUid();
        waitUntilCondition(gone(serviceAccess, uid), ofSeconds(5), ofMinutes(1));

        // wait for the service to appear

        waitUntilCondition(not(gone(serviceAccess)), ofSeconds(5), ofMinutes(1));

        // wait for the messaging endpoint to become ready

        var endpointAccess = Kubernetes.messagingEndpoints(endpoint.getMetadata().getNamespace())
                .withName(endpoint.getMetadata().getName());
        waitUntilCondition(condition(endpointAccess, "Ready"), ofSeconds(5), ofMinutes(1));

        // test by sending messages

        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    private void createEndpointsAndAddress(ExtensionContext extensionContext, String addressName, String namespace, MessagingEndpoint... endpoints) throws InterruptedException {
        MessagingAddress address = new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withNamespace(namespace)
                .withName(addressName)
                .endMetadata()
                .editOrNewSpec()
                .editOrNewQueue()
                .endQueue()
                .endSpec()
                .build();

        resourceManager.createResource(extensionContext, endpoints);
        resourceManager.createResource(extensionContext, address);

        for (MessagingEndpoint endpoint : endpoints) {
            endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
            MessagingEndpointCondition endpointCondition = MessagingEndpointResourceType.getCondition(endpoint.getStatus().getConditions(), "Ready");
            assertNotNull(endpointCondition);
            assertEquals("True", endpointCondition.getStatus());
        }

        address = MessagingAddressResourceType.getOperation().inNamespace(address.getMetadata().getNamespace()).withName(address.getMetadata().getName()).get();
        MessagingAddressCondition addressCondition = MessagingAddressResourceType.getCondition(address.getStatus().getConditions(), "Ready");
        assertNotNull(addressCondition);
        assertEquals("True", addressCondition.getStatus());
    }
}
