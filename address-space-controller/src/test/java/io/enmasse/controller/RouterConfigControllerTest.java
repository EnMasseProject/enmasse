/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.router.config.Listener;
import io.enmasse.controller.router.config.RouterConfig;
import io.enmasse.controller.router.config.VhostPolicy;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.model.CustomResourceDefinitions;
import io.fabric8.kubernetes.api.model.ConfigMap;
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
}
