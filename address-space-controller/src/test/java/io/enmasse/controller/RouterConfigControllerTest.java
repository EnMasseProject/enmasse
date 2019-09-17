/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceStatusConnector;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.router.config.*;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.model.CustomResourceDefinitions;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouterConfigControllerTest {

    private NamespacedKubernetesClient client;

    public KubernetesServer kubernetesServer = new KubernetesServer(false, true);

    private AuthenticationServiceRegistry authenticationServiceRegistry;

    @BeforeAll
    public static void init() {
        CustomResourceDefinitions.registerAll();
    }

    @BeforeEach
    public void setup() {
        kubernetesServer.before();
        client = kubernetesServer.getClient();
        authenticationServiceRegistry = mock(AuthenticationServiceRegistry.class);
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(
                new AuthenticationServiceBuilder()
                        .editOrNewMetadata()
                        .withName("test")
                        .endMetadata()
                        .editOrNewSpec()
                        .withType(AuthenticationServiceType.standard)
                        .withRealm("myrealm")
                        .endSpec()
                        .editOrNewStatus()
                        .withHost("auth.example.com")
                        .withPort(5671)
                        .endStatus()
                        .build()));
    }

    @Test
    public void testReconcile() throws Exception {
        RouterConfigController configController = new RouterConfigController(
                client,
                "test",
                new AuthenticationServiceResolver(authenticationServiceRegistry));

        StandardInfraConfig appliedConfig = new StandardInfraConfigBuilder()
                .editOrNewMetadata()
                .withName("test")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewRouter()
                .withIdleTimeout(2)
                .withLinkCapacity(50)
                .withHandshakeTimeout(20)
                .withNewPolicy()
                .withMaxConnections(30)
                .withMaxConnectionsPerHost(10)
                .withMaxConnectionsPerUser(10)
                .withMaxReceiversPerConnection(2)
                .withMaxSendersPerConnection(3)
                .withMaxSessionsPerConnection(4)
                .endPolicy()
                .endRouter()
                .endSpec()
                .build();

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withName("myspace")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .withType("type1")
                .withPlan("plan1")
                .withNewAuthenticationService()
                .withName("test")
                .endAuthenticationService()
                .endSpec()
                .build();
        InfraConfigs.setCurrentInfraConfig(addressSpace, appliedConfig);

        configController.reconcile(addressSpace);

        ConfigMap routerConfigMap = client.configMaps().inNamespace("test").withName("qdrouterd-config.1234").get();
        assertNotNull(routerConfigMap);

        RouterConfig actual = RouterConfig.fromMap(routerConfigMap.getData());

        assertEquals("${HOSTNAME}", actual.getRouter().getId());
        assertEquals(3, actual.getSslProfiles().size());
        assertEquals(8, actual.getListeners().size());
        assertEquals(2, actual.getLinkRoutes().size());
        assertEquals(3, actual.getAddresses().size());
        assertEquals(1, actual.getConnectors().size());
        assertEquals(1, actual.getPolicies().size());
        assertEquals(2, actual.getVhosts().size());
        assertEquals(1, actual.getAuthServicePlugins().size());

        Listener amqpPublic = getListenerOnPort(5672, actual.getListeners());
        assertNotNull(amqpPublic);
        assertEquals(2, amqpPublic.getIdleTimeoutSeconds());
        assertEquals(20, amqpPublic.getInitialHandshakeTimeoutSeconds());
        assertEquals(50, amqpPublic.getLinkCapacity());

        VhostPolicy internal = getPolicyForHostname("$${dummy}default", actual.getVhosts());
        assertNotNull(internal);
        assertNull(internal.getMaxConnections());
        assertNull(internal.getMaxConnectionsPerHost());
        assertNull(internal.getMaxConnectionsPerUser());
        assertNull(internal.getGroups().get("$${dummy}default").getMaxSessions());
        assertNull(internal.getGroups().get("$${dummy}default").getMaxSenders());
        assertNull(internal.getGroups().get("$${dummy}default").getMaxReceivers());

        VhostPolicy pub = getPolicyForHostname("public", actual.getVhosts());
        assertNotNull(pub);
        assertEquals(30, pub.getMaxConnections());
        assertEquals(10, pub.getMaxConnectionsPerUser());
        assertEquals(10, pub.getMaxConnectionsPerHost());
        assertEquals(4, pub.getGroups().get("$${dummy}default").getMaxSessions());
        assertEquals(3, pub.getGroups().get("$${dummy}default").getMaxSenders());
        assertEquals(2, pub.getGroups().get("$${dummy}default").getMaxReceivers());


        appliedConfig.getSpec().getRouter().setIdleTimeout(20);
        appliedConfig.getSpec().getRouter().getPolicy().setMaxConnectionsPerUser(300);
        InfraConfigs.setCurrentInfraConfig(addressSpace, appliedConfig);

        configController.reconcile(addressSpace);

        routerConfigMap = client.configMaps().inNamespace("test").withName("qdrouterd-config.1234").get();
        assertNotNull(routerConfigMap);

        actual = RouterConfig.fromMap(routerConfigMap.getData());
        amqpPublic = getListenerOnPort(5672, actual.getListeners());
        assertNotNull(amqpPublic);
        assertEquals(20, amqpPublic.getIdleTimeoutSeconds());
        assertEquals(20, amqpPublic.getInitialHandshakeTimeoutSeconds());
        assertEquals(50, amqpPublic.getLinkCapacity());

        pub = getPolicyForHostname("public", actual.getVhosts());
        assertNotNull(pub);
        assertEquals(30, pub.getMaxConnections());
        assertEquals(300, pub.getMaxConnectionsPerUser());
    }

    @Test
    public void testReconcileConnector() throws Exception {
        RouterConfigController configController = new RouterConfigController(
                client,
                "test",
                new AuthenticationServiceResolver(authenticationServiceRegistry));

        StandardInfraConfig appliedConfig = new StandardInfraConfigBuilder()
                .editOrNewMetadata()
                .withName("test")
                .endMetadata()
                .build();

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withName("myspace")
                .withNamespace("space")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .withType("type1")
                .withPlan("plan1")
                .withNewAuthenticationService()
                .withName("test")
                .endAuthenticationService()
                .addNewConnector()
                .withName("remote1")
                .addNewEndpointHost()
                .withHost("messaging.example.com")
                .withPort(5671)
                .endEndpointHost()
                .addNewEndpointHost()
                .withHost("messaging2.example.com")
                .endEndpointHost()

                .withNewTls()
                .withNewCaCert()
                .withNewValueFromSecret("ca.crt", "remote-certs", false)
                .endCaCert()
                .withNewClientCert()
                .withNewValueFromSecret("tls.crt", "remote-certs", false)
                .endClientCert()
                .withNewClientKey()
                .withNewValueFromSecret("tls.key", "remote-certs", false)
                .endClientKey()
                .endTls()
                .withNewCredentials()
                .withNewUsername()
                .withValue("test")
                .endUsername()
                .withNewPassword()
                .withValue("test")
                .endPassword()
                .endCredentials()

                .addNewAddress()
                .withName("pat1")
                .withPattern("foo*")
                .endAddress()
                .endConnector()
                .endSpec()
                .build();


        InfraConfigs.setCurrentInfraConfig(addressSpace, appliedConfig);

        /*
        client.apps().statefulSets().inNamespace("test").createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(KubeUtil.getRouterSetName(addressSpace))
                .withNamespace("test")
                .endMetadata()
                .editOrNewSpec()
                .withReplicas(1)
                .editOrNewTemplate()
                .editOrNewSpec()
                .addNewContainer()
                .withName("router")
                .addNewVolumeMount()
                .withName("external-connector-old")
                .endVolumeMount()
                .endContainer()
                .addNewVolume()
                .withName("external-connector-old")
                .withNewSecret()
                .withSecretName("external-connector-old")
                .endSecret()
                .endVolume()
                .endSpec()
                .endTemplate()
                .endSpec()
                .done();
                */

        configController.reconcile(addressSpace);

        AddressSpaceStatusConnector status = addressSpace.getStatus().getConnectors().get(0);
        assertNotNull(status);
        assertEquals("remote1", status.getName());
        assertFalse(status.isReady());
        assertTrue(status.getMessages().contains("Unable to locate value or secret for caCert"));
        assertTrue(status.getMessages().contains("Unable to locate value or secret for clientCert"));
        assertTrue(status.getMessages().contains("Unable to locate value or secret for clientKey"));


        ConfigMap routerConfigMap = client.configMaps().inNamespace("test").withName("qdrouterd-config.1234").get();
        assertNotNull(routerConfigMap);

        RouterConfig actual = RouterConfig.fromMap(routerConfigMap.getData());

        SslProfile profile = getSslProfile("connector_remote1_settings", actual.getSslProfiles());
        assertNull(profile);

        Connector remote = getConnectorForHost("messaging.example.com", actual.getConnectors());
        assertNull(remote);

        status.setReady(true);
        status.clearMessages();

        client.secrets().inNamespace("space").createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName("remote-certs")
                .endMetadata()
                .addToData("tls.crt", "cert")
                .addToData("tls.key", "key")
                .addToData("ca.crt", "ca")
                .done();

        configController.reconcile(addressSpace);


        routerConfigMap = client.configMaps().inNamespace("test").withName("qdrouterd-config.1234").get();
        assertNotNull(routerConfigMap);

        actual = RouterConfig.fromMap(routerConfigMap.getData());

        profile = getSslProfile("connector_remote1_settings", actual.getSslProfiles());
        assertNotNull(profile);
        assertEquals("connector_remote1_settings", profile.getName());
        assertEquals("/etc/enmasse-connectors/remote1/ca.crt", profile.getCaCertFile());
        assertEquals("/etc/enmasse-connectors/remote1/tls.crt", profile.getCertFile());
        assertEquals("/etc/enmasse-connectors/remote1/tls.key", profile.getPrivateKeyFile());

        remote = getConnectorForHost("messaging.example.com", actual.getConnectors());
        assertNotNull(remote);
        assertEquals("amqps://messaging2.example.com:5671", remote.getFailoverUrls());
        assertEquals("connector_remote1_settings", remote.getSslProfile());
        assertEquals("EXTERNAL PLAIN", remote.getSaslMechanisms());
        assertEquals(5671, remote.getPort());
        assertEquals("test", remote.getSaslUsername());
        assertEquals("test", remote.getSaslPassword());

        LinkRoute lrIn = getLinkRoute("override.connector.remote1.pat1.in", actual.getLinkRoutes());
        assertNotNull(lrIn);
        assertEquals("remote1/foo*", lrIn.getPattern());
        assertEquals(LinkRoute.Direction.in, lrIn.getDirection());
        assertEquals("remote1", lrIn.getConnection());

        LinkRoute lrOut = getLinkRoute("override.connector.remote1.pat1.out", actual.getLinkRoutes());
        assertNotNull(lrOut);
        assertEquals("remote1/foo*", lrOut.getPattern());
        assertEquals(LinkRoute.Direction.out, lrOut.getDirection());
        assertEquals("remote1", lrOut.getConnection());


        status = addressSpace.getStatus().getConnectors().get(0);
        assertNotNull(status);
        assertEquals("remote1", status.getName());
        assertTrue(status.isReady());

        Secret certs = client.secrets().inNamespace("test").withName("external-connector-1234-remote1").get();
        assertNotNull(certs);
        assertEquals("ca", certs.getData().get("ca.crt"));
        assertEquals("key", certs.getData().get("tls.key"));
        assertEquals("cert", certs.getData().get("tls.crt"));

        /*
        StatefulSet router = client.apps().statefulSets().inNamespace("test").withName("qdrouterd-1234").get();
        assertNotNull(router);
        */
    }


    @Test
    public void testVhostPolicyGen() {
        RouterPolicySpec policySpec = new RouterPolicySpecBuilder()
                .withMaxConnections(1000)
                .withMaxConnectionsPerHost(10)
                .withMaxConnectionsPerUser(10)
                .withMaxSendersPerConnection(5)
                .withMaxReceiversPerConnection(5)
                .withMaxSessionsPerConnection(5)
                .build();

        List<VhostPolicy> policyList = RouterConfigController.createVhostPolices(policySpec);

        assertEquals(2, policyList.size());

        VhostPolicy internal = getPolicyForHostname("$${dummy}default", policyList);
        assertNotNull(internal);
        assertNull(internal.getMaxConnections());
        assertNull(internal.getMaxConnectionsPerHost());
        assertNull(internal.getMaxConnectionsPerUser());
        assertNull(internal.getGroups().get("$${dummy}default").getMaxSessions());
        assertNull(internal.getGroups().get("$${dummy}default").getMaxSenders());
        assertNull(internal.getGroups().get("$${dummy}default").getMaxReceivers());

        VhostPolicy pub = getPolicyForHostname("public", policyList);
        assertNotNull(pub);
        assertEquals(1000, pub.getMaxConnections());
        assertEquals(10, pub.getMaxConnectionsPerUser());
        assertEquals(10, pub.getMaxConnectionsPerHost());
        assertEquals(5, pub.getGroups().get("$${dummy}default").getMaxSessions());
        assertEquals(5, pub.getGroups().get("$${dummy}default").getMaxSenders());
        assertEquals(5, pub.getGroups().get("$${dummy}default").getMaxReceivers());
    }

    private Listener getListenerOnPort(int port, List<Listener> listeners) {
        for (Listener listener : listeners) {
            if (port == listener.getPort()) {
                return listener;
            }
        }
        return null;
    }

    private VhostPolicy getPolicyForHostname(String hostname, List<VhostPolicy> vhostPolicies) {
        for (VhostPolicy vhostPolicy : vhostPolicies) {
            if (hostname.equals(vhostPolicy.getHostname())) {
                return vhostPolicy;
            }
        }
        return null;
    }

    private SslProfile getSslProfile(String name, List<SslProfile> sslProfiles) {
        for (SslProfile sslProfile : sslProfiles) {
            if (name.equals(sslProfile.getName())) {
                return sslProfile;
            }
        }
        return null;
    }

    private Connector getConnectorForHost(String hostname, List<Connector> connectors) {
        for (Connector connector : connectors) {
            if (hostname.equals(connector.getHost())) {
                return connector;
            }
        }
        return null;
    }

    private LinkRoute getLinkRoute(String name, List<LinkRoute> linkRoutes) {
        for (LinkRoute lr : linkRoutes) {
            if (name.equals(lr.getName())) {
                return lr;
            }
        }
        return null;
    }
}
