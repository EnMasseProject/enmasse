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
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.certs.openssl.OpenSSLUtil;
import io.enmasse.systemtest.condition.Kubernetes;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingProject;
import io.enmasse.systemtest.framework.annotations.ExternalClients;
import io.enmasse.systemtest.framework.annotations.ParallelTest;
import io.enmasse.systemtest.messaginginfra.resources.MessagingAddressResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType;
import io.enmasse.systemtest.utils.AssertionUtils;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DefaultMessagingInfrastructure
@DefaultMessagingProject
@ExternalClients
public class MessagingEndpointTest extends TestBase {
    @ParallelTest
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
    }

    @ParallelTest
    public void testClusterEndpoint(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-cluster-endpoint")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTls()
                .editOrNewSelfsigned()
                .endSelfsigned()
                .endTls()
                .editOrNewCluster()
                .endCluster()
                .addToProtocols("AMQP", "AMQPS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue2", project.getMetadata().getNamespace(), endpoint);

        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP", endpoint), "queue2", false, false);
        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue2", true, false);
        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    @ParallelTest
    @OpenShift
    public void testRouteEndpoint(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-route")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewRoute()
                .endRoute()
                .editOrNewTls()
                .editOrNewSelfsigned()
                .endSelfsigned()
                .endTls()
                .addToProtocols("AMQPS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue3", project.getMetadata().getNamespace(), endpoint);

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCaCertificate());

        AmqpClient client = clientRunner.sendReceiveOutsideCluster(endpoint.getStatus().getHost(),
                MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue3", true, true, endpoint.getStatus().getTls().getCaCertificate());
        assertMessagingOutside(client, "queue3");
    }

    @ParallelTest
    @Kubernetes
    public void testLoadBalancerEndpoint(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-load-balancer")
                .endMetadata()
                .editOrNewSpec()
                .withHost(kubernetes.getHost())
                .editOrNewLoadBalancer()
                .endLoadBalancer()
                .addToProtocols("AMQP")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue4", project.getMetadata().getNamespace(), endpoint);

        AmqpClient client = clientRunner.sendReceiveOutsideCluster(endpoint.getStatus().getHost(),
                MessagingEndpointResourceType.getPort("AMQP", endpoint), "queue4", false, false, null);
        assertMessagingOutside(client, "queue4");
    }

    /**
     * Test requires ingress controller with SSL passthrough enabled.
     */
    @ParallelTest
    @Kubernetes
    public void testIngressEndpoint(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-ingress")
                .endMetadata()
                .editOrNewSpec()
                .addToAnnotations("nginx.ingress.kubernetes.io/ssl-passthrough", "true")
                .withHost(String.format("%s.nip.io", kubernetes.getHost()))
                .editOrNewIngress()
                .endIngress()
                .editOrNewTls()
                .editOrNewSelfsigned()
                .endSelfsigned()
                .endTls()
                .addToProtocols("AMQPS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue5", project.getMetadata().getNamespace(), endpoint);

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCaCertificate());

        AmqpClient client = clientRunner.sendReceiveOutsideCluster(endpoint.getStatus().getHost(),
                MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue5", true, true, endpoint.getStatus().getTls().getCaCertificate());
        assertMessagingOutside(client, "queue5");
    }

    @ParallelTest
    @OpenShift(version = OpenShiftVersion.OCP4)
    public void testOpenShiftCert(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-ocp-cert")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTls()
                .editOrNewOpenshift()
                .endOpenshift()
                .endTls()
                .editOrNewCluster()
                .endCluster()
                .addToProtocols("AMQPS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue6", project.getMetadata().getNamespace(), endpoint);
        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue6", true, false);
        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    @ParallelTest
    @OpenShift
    public void testSelfsignedCert(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-self-signed")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewRoute()
                .endRoute()
                .editOrNewTls()
                .editOrNewSelfsigned()
                .endSelfsigned()
                .endTls()
                .addToProtocols("AMQPS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue7", project.getMetadata().getNamespace(), endpoint);

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCaCertificate());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity().getNotBefore());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity().getNotAfter());
        assertFalse(endpoint.getStatus().getTls().getCertificateValidity().getNotBefore().isEmpty());
        assertFalse(endpoint.getStatus().getTls().getCertificateValidity().getNotAfter().isEmpty());

        AmqpClient client = clientRunner.sendReceiveOutsideCluster(endpoint.getStatus().getHost(),
                MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue7", true, true, endpoint.getStatus().getTls().getCaCertificate());
        assertMessagingOutside(client, "queue7");
    }

    @ParallelTest
    @OpenShift
    public void testExternalCert(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        CertBundle messagingCert = OpenSSLUtil.createCertBundle("messaging.example.com");
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-external-cert")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewRoute()
                .endRoute()
                .editOrNewTls()
                .editOrNewExternal()
                .editOrNewCertificate()
                .withValue(messagingCert.getCert())
                .endCertificate()
                .editOrNewKey()
                .withValue(messagingCert.getKey())
                .endKey()
                .endExternal()
                .endTls()
                .addToProtocols("AMQPS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue8", project.getMetadata().getNamespace(), endpoint);

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity().getNotBefore());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity().getNotAfter());

        // Disable host verification as we cant verify that. However as long as cert is valid that should be sufficient to validate it is being set correctly.
        AmqpClient client = clientRunner.sendReceiveOutsideCluster(endpoint.getStatus().getHost(),
                MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue8", true, false, messagingCert.getCaCert());
        assertMessagingOutside(client, "queue8");
    }

    @ParallelTest
    public void testClusterEndpointWebsockets(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-ws1")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTls()
                .editOrNewSelfsigned()
                .endSelfsigned()
                .endTls()
                .editOrNewCluster()
                .endCluster()
                .addToProtocols("AMQP-WS", "AMQP-WSS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue9", project.getMetadata().getNamespace(), endpoint);

        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(),MessagingEndpointResourceType.getPort("AMQP-WS", endpoint), "queue9", false, true);
        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WSS", endpoint), "queue9", true, true);
        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    @ParallelTest
    @Kubernetes
    public void testIngressEndpointWebsocket(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-ws2")
                .endMetadata()
                .editOrNewSpec()
                .withHost(String.format("%s.nip.io", kubernetes.getHost()))
                .editOrNewIngress()
                .endIngress()
                .addToProtocols("AMQP-WS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue10", project.getMetadata().getNamespace(), endpoint);
        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WS", endpoint), "queue10", false, true);
        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    @ParallelTest
    @OpenShift
    public void testRouteEndpointWebsocket(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-ws3")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewRoute()
                .endRoute()
                .addToProtocols("AMQP-WS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue11", project.getMetadata().getNamespace(), endpoint);
        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WS", endpoint), "queue11", false, true);
        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    @ParallelTest
    @OpenShift
    public void testRouteEndpointWebsocketTlsPassthrough(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-ws4")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTls()
                .editOrNewSelfsigned()
                .endSelfsigned()
                .endTls()
                .editOrNewRoute()
                .endRoute()
                .addToProtocols("AMQP-WSS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue12", project.getMetadata().getNamespace(), endpoint);
        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WSS", endpoint), "queue12", true, true);
        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    @ParallelTest
    @OpenShift(version = OpenShiftVersion.OCP3)
    public void testRouteEndpointWebsocketTlsReencrypt(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-ws5")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTls()
                .editOrNewSelfsigned()
                .endSelfsigned()
                .endTls()
                .editOrNewRoute()
                .withTlsTermination("reencrypt")
                .endRoute()
                .addToProtocols("AMQP-WSS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue13", project.getMetadata().getNamespace(), endpoint);
        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WSS", endpoint), "queue13", true, true);
        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    @ParallelTest
    @Disabled("Awaiting fixes in DISPATCH-1585 to allow endpoints to use same addresses")
    public void testMultipleEndpoints(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();

        MessagingEndpoint endpoint1 = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("endpoint1")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewCluster()
                .endCluster()
                .addToProtocols("AMQP")
                .endSpec()
                .build();

        MessagingEndpoint endpoint2 = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("endpoint2")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewCluster()
                .endCluster()
                .addToProtocols("AMQP")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue14", project.getMetadata().getNamespace(), endpoint1, endpoint2);

        clientRunner.send(endpoint1, "queue14");
        clientRunner.receive(endpoint2, "queue14");

        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    @ParallelTest
    @OpenShift
    public void testRouteEndpointWebsocketTlsEdge(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app-ws6")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewRoute()
                .withTlsTermination("edge")
                .endRoute()
                .addToProtocols("AMQP-WSS")
                .endSpec()
                .build();

        createEndpointsAndAddress(extensionContext, "queue15", project.getMetadata().getNamespace(), endpoint);
        clientRunner.sendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WSS", endpoint), "queue15", true, true);
        AssertionUtils.assertDefaultMessaging(clientRunner);
    }

    private void assertMessagingOutside(AmqpClient client, String address) throws ExecutionException, InterruptedException {
        var result = client.recvMessages(address, 1).get();
        assertEquals(1, result.size());
        assertEquals("hello", ((AmqpValue) result.get(0).getBody()).getValue());
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
