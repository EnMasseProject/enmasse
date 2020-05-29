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
import io.enmasse.api.model.MessagingEndpointPort;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpConnectOptions;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.annotations.DefaultMessagingTenant;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedSharedInfra;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.certs.openssl.OpenSSLUtil;
import io.enmasse.systemtest.condition.Kubernetes;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.messaginginfra.resources.MessagingAddressResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType;
import io.enmasse.systemtest.platform.cluster.ClusterType;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.ISOLATED_SHARED_INFRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(ISOLATED_SHARED_INFRA)
@DefaultMessagingInfrastructure
@DefaultMessagingTenant
@ExternalClients
public class MessagingEndpointTest extends TestBase implements ITestIsolatedSharedInfra {

    @Test
    @Kubernetes(type = ClusterType.MINIKUBE)
    public void testNodePortEndpointMinikube() throws Exception {
        testNodePortEndpoint();
    }

    @Test
    @OpenShift(type = ClusterType.CRC)
    public void testNodePortEndpointCRC() throws Exception {
        testNodePortEndpoint();
    }

    private void testNodePortEndpoint() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
                .endMetadata()
                .editOrNewSpec()
                .withHost(kubernetes.getHost())
                .addToProtocols("AMQP")
                .editOrNewNodePort()
                .endNodePort()
                .endSpec()
                .build();

        createEndpointAndAddress(endpoint, "queue1");

        doTestSendReceiveOutsideCluster(endpoint.getStatus().getHost(), getPort("AMQP", endpoint), "queue1", false, false, null);
    }

    @Test
    public void testClusterEndpoint() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
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

        createEndpointAndAddress(endpoint, "queue1");

        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQP", endpoint), "queue1", false, false);
        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQPS", endpoint), "queue1", true, false);
    }

    @Test
    @OpenShift
    public void testRouteEndpoint() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
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

        createEndpointAndAddress(endpoint, "queue1");

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCaCertificate());

        doTestSendReceiveOutsideCluster(endpoint.getStatus().getHost(), getPort("AMQPS", endpoint), "queue1", true, true, endpoint.getStatus().getTls().getCaCertificate());
    }

    @Test
    @Kubernetes
    public void testLoadBalancerEndpoint() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
                .endMetadata()
                .editOrNewSpec()
                .withHost(kubernetes.getHost())
                .editOrNewLoadBalancer()
                .endLoadBalancer()
                .addToProtocols("AMQP")
                .endSpec()
                .build();

        createEndpointAndAddress(endpoint, "queue1");

        doTestSendReceiveOutsideCluster(endpoint.getStatus().getHost(), getPort("AMQP", endpoint), "queue1", false, false, null);
    }

    /**
     * Test requires ingress controller with SSL passthrough enabled.
     */
    @Test
    @Kubernetes
    public void testIngressEndpoint() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
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

        createEndpointAndAddress(endpoint, "queue1");

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCaCertificate());

        doTestSendReceiveOutsideCluster(endpoint.getStatus().getHost(), getPort("AMQPS", endpoint), "queue1", true, true, endpoint.getStatus().getTls().getCaCertificate());
    }

    @Test
    @OpenShift(version = OpenShiftVersion.OCP4)
    public void testOpenShiftCert() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
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

        createEndpointAndAddress(endpoint, "queue1");
        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQPS", endpoint), "queue1", true, false);
    }

    @Test
    @OpenShift
    public void testSelfsignedCert() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
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

        createEndpointAndAddress(endpoint, "queue1");

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCaCertificate());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity().getNotBefore());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity().getNotAfter());
        assertFalse(endpoint.getStatus().getTls().getCertificateValidity().getNotBefore().isEmpty());
        assertFalse(endpoint.getStatus().getTls().getCertificateValidity().getNotAfter().isEmpty());

        doTestSendReceiveOutsideCluster(endpoint.getStatus().getHost(), getPort("AMQPS", endpoint), "queue1", true, true, endpoint.getStatus().getTls().getCaCertificate());
    }

    @Test
    @OpenShift
    public void testExternalCert() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        CertBundle messagingCert = OpenSSLUtil.createCertBundle("messaging.example.com");
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
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

        createEndpointAndAddress(endpoint, "queue1");

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        assertNotNull(endpoint.getStatus().getTls());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity().getNotBefore());
        assertNotNull(endpoint.getStatus().getTls().getCertificateValidity().getNotAfter());

        // Disable host verification as we cant verify that. However as long as cert is valid that should be sufficient to validate it is being set correctly.
        doTestSendReceiveOutsideCluster(endpoint.getStatus().getHost(), getPort("AMQPS", endpoint), "queue1", true, false, messagingCert.getCaCert());
    }

    @Test
    public void testClusterEndpointWebsockets() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
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

        createEndpointAndAddress(endpoint, "queue1");

        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQP-WS", endpoint), "queue1", false, true);
        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQP-WSS", endpoint), "queue1", true, true);
    }

    @Test
    @Kubernetes
    public void testIngressEndpointWebsocket() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app-ws2")
                .endMetadata()
                .editOrNewSpec()
                .withHost(String.format("%s.nip.io", kubernetes.getHost()))
                .editOrNewIngress()
                .endIngress()
                .addToProtocols("AMQP-WS")
                .endSpec()
                .build();

        createEndpointAndAddress(endpoint, "queue1");
        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQP-WS", endpoint), "queue1", false, true);
    }

    @Test
    @OpenShift
    public void testRouteEndpointWebsocket() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app-ws3")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewRoute()
                .endRoute()
                .addToProtocols("AMQP-WS")
                .endSpec()
                .build();

        createEndpointAndAddress(endpoint, "queue1");
        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQP-WS", endpoint), "queue1", false, true);
    }

    @Test
    @OpenShift
    public void testRouteEndpointWebsocketTlsPassthrough() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
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

        createEndpointAndAddress(endpoint, "queue1");
        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQP-WSS", endpoint), "queue1", true, true);
    }

    @Test
    @OpenShift(version = OpenShiftVersion.OCP3)
    public void testRouteEndpointWebsocketTlsReencrypt() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
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

        createEndpointAndAddress(endpoint, "queue1");
        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQP-WSS", endpoint), "queue1", true, true);
    }

    @Test
    @OpenShift
    public void testRouteEndpointWebsocketTlsEdge() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app-ws6")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewRoute()
                .withTlsTermination("edge")
                .endRoute()
                .addToProtocols("AMQP-WSS")
                .endSpec()
                .build();

        createEndpointAndAddress(endpoint, "queue1");
        doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), getPort("AMQP-WSS", endpoint), "queue1", true, true);
    }

    private static int getPort(String protocol, MessagingEndpoint endpoint) {
        for (MessagingEndpointPort port : endpoint.getStatus().getPorts()) {
            if (protocol.equals(port.getProtocol()))  {
                return port.getPort();
            }
        }
        return 0;
    }

    void doTestSendReceiveOutsideCluster(String host, int port, String address, boolean tls, boolean verifyHost, String caCert) throws Exception {
        ProtonClientOptions protonClientOptions = new ProtonClientOptions();
        if (tls) {
            protonClientOptions.setSsl(true);
            if (!verifyHost) {
                protonClientOptions.setHostnameVerificationAlgorithm("");
            }
            if (caCert != null) {
                protonClientOptions.setTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(caCert)));
            }
        }
        AmqpClient client = infraResourceManager.getAmqpClientFactory().createClient(new AmqpConnectOptions()
                .setSaslMechanism("ANONYMOUS")
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setEndpoint(new Endpoint(host, port))
                .setProtonClientOptions(protonClientOptions)
                .setTerminusFactory(new QueueTerminusFactory()));

        assertEquals(1, client.sendMessages(address, Collections.singletonList("hello")).get(1, TimeUnit.MINUTES));
        var result = client.recvMessages(address, 1).get();
        assertEquals(1, result.size());
        assertEquals("hello", ((AmqpValue) result.get(0).getBody()).getValue());
    }

    static void doTestSendReceiveOnCluster(String host, int port, String address, boolean enableTls, boolean websockets) throws Exception {
        assertTrue(port > 0);
        int expectedMsgCount = 10;
        Endpoint e = new Endpoint(host, port);
        ExternalMessagingClient senderClient = new ExternalMessagingClient(enableTls)
                .withClientEngine(websockets ? new RheaClientSender() : new ProtonJMSClientSender())
                .withMessagingRoute(e)
                .withAddress(address)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                .withTimeout(30);

        ExternalMessagingClient receiverClient = new ExternalMessagingClient(enableTls)
                .withClientEngine(websockets ? new RheaClientReceiver() : new ProtonJMSClientReceiver())
                .withMessagingRoute(e)
                .withAddress(address)
                .withCount(expectedMsgCount)
                .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                .withTimeout(30);

/*        if (enableTls) {
            senderClient.withAdditionalArgument(ClientArgument.CONN_SSL_VERIFY_PEER_NAME, true);
        }*/
        if (websockets) {
            senderClient.withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET, true);
            receiverClient.withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET, true);
            senderClient.withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS, "binary");
            receiverClient.withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS, "binary");
        }

        List<Future<Boolean>> results = ForkJoinPool.commonPool().invokeAll(List.of(senderClient::run, receiverClient::run));

        assertTrue(results.get(0).get(1, TimeUnit.MINUTES), "Sender failed, expected return code 0");
        assertTrue(results.get(1).get(1, TimeUnit.MINUTES), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
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

        infraResourceManager.createResource(endpoint, address);

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
