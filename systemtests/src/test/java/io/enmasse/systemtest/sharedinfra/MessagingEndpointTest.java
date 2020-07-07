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
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpConnectOptions;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.certs.openssl.OpenSSLUtil;
import io.enmasse.systemtest.condition.Kubernetes;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingProject;
import io.enmasse.systemtest.framework.annotations.ExternalClients;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.messaginginfra.resources.MessagingAddressResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DefaultMessagingInfrastructure
@DefaultMessagingProject
@ExternalClients
public class MessagingEndpointTest extends TestBase {
    @Test
    public void testNodePortEndpoint() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue1");

        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP", endpoint), "queue1", false, false);
    }

    @Test
    public void testClusterEndpoint() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue2");

        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP", endpoint), "queue2", false, false);
        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue2", true, false);
        assertDefaultMessaging();
    }

    @Test
    @OpenShift
    public void testRouteEndpoint() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue3");

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCaCertificate());

        AmqpClient client = clientRunner.sendReceiveOutsideCluster(endpoint.getStatus().getHost(),
                MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue3", true, true, endpoint.getStatus().getTls().getCaCertificate());
        assertMessagingOutside(client, "queue3");
    }

    @Test
    @Kubernetes
    public void testLoadBalancerEndpoint() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue4");

        AmqpClient client = clientRunner.sendReceiveOutsideCluster(endpoint.getStatus().getHost(),
                MessagingEndpointResourceType.getPort("AMQP", endpoint), "queue4", false, false, null);
        assertMessagingOutside(client, "queue4");
    }

    /**
     * Test requires ingress controller with SSL passthrough enabled.
     */
    @Test
    @Kubernetes
    public void testIngressEndpoint() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue5");

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCaCertificate());

        AmqpClient client = clientRunner.sendReceiveOutsideCluster(endpoint.getStatus().getHost(),
                MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue5", true, true, endpoint.getStatus().getTls().getCaCertificate());
    }

    @Test
    @OpenShift(version = OpenShiftVersion.OCP4)
    public void testOpenShiftCert() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue6");
        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQPS", endpoint), "queue6", true, false);
    }

    @Test
    @OpenShift
    public void testSelfsignedCert() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue7");

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

    @Test
    @OpenShift
    public void testExternalCert() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue8");

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

    @Test
    public void testClusterEndpointWebsockets() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue9");

        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(),MessagingEndpointResourceType.getPort("AMQP-WS", endpoint), "queue9", false, true);
        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WSS", endpoint), "queue9", true, true);
        assertDefaultMessaging();
    }

    @Test
    @Kubernetes
    public void testIngressEndpointWebsocket() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue10");
        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WS", endpoint), "queue10", false, true);
        assertDefaultMessaging();
    }

    @Test
    @OpenShift
    public void testRouteEndpointWebsocket() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue11");
        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WS", endpoint), "queue11", false, true);
        assertDefaultMessaging();
    }

    @Test
    @OpenShift
    public void testRouteEndpointWebsocketTlsPassthrough() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue12");
        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WSS", endpoint), "queue12", true, true);
        assertDefaultMessaging();
    }

    @Test
    @OpenShift(version = OpenShiftVersion.OCP3)
    public void testRouteEndpointWebsocketTlsReencrypt() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue13");
        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WSS", endpoint), "queue13", true, true);
        assertDefaultMessaging();
    }

    @Test
    @OpenShift
    public void testRouteEndpointWebsocketTlsEdge() throws Exception {
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

        createEndpointAndAddress(endpoint, "queue14");
        clientRunner.endpointSendAndReceiveOnCluster(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP-WSS", endpoint), "queue14", true, true);
        assertDefaultMessaging();
    }

    private void assertMessagingOutside(AmqpClient client, String address) throws ExecutionException, InterruptedException {
        var result = client.recvMessages(address, 1).get();
        assertEquals(1, result.size());
        assertEquals("hello", ((AmqpValue) result.get(0).getBody()).getValue());
    }

    private void createEndpointAndAddress(MessagingEndpoint endpoint, String addressName) {
        MessagingAddress address = new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withNamespace(endpoint.getMetadata().getNamespace())
                .withName(addressName)
                .endMetadata()
                .editOrNewSpec()
                .editOrNewQueue()
                .endQueue()
                .endSpec()
                .build();

        resourceManager.createResource(endpoint, address);

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        MessagingEndpointCondition endpointCondition = MessagingEndpointResourceType.getCondition(endpoint.getStatus().getConditions(), "Ready");
        assertNotNull(endpointCondition);
        assertEquals("True", endpointCondition.getStatus());

        address = MessagingAddressResourceType.getOperation().inNamespace(address.getMetadata().getNamespace()).withName(address.getMetadata().getName()).get();
        MessagingAddressCondition addressCondition = MessagingAddressResourceType.getCondition(address.getStatus().getConditions(), "Ready");
        assertNotNull(addressCondition);
        assertEquals("True", addressCondition.getStatus());
    }
}
