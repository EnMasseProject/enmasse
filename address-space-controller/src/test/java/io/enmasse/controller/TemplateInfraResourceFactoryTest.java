/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AuthenticationServiceResolver;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.EndpointSpecBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecAdminBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecBrokerBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecRouterBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.KubernetesHelper;
import io.enmasse.k8s.util.JULInitializingTest;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;

public class TemplateInfraResourceFactoryTest extends JULInitializingTest {

    private OpenShiftServer openShiftServer = new OpenShiftServer(false, true);

    private TemplateInfraResourceFactory resourceFactory;
    private NamespacedOpenShiftClient client;

    @AfterEach
    void tearDown() {
        openShiftServer.after();
    }

    @BeforeEach
    public void setup() {
        openShiftServer.before();
        client = openShiftServer.getOpenshiftClient();
        client.secrets().createNew().editOrNewMetadata().withName("certs").endMetadata().addToData("tls.crt", "cert").done();
        AuthenticationServiceResolver authServiceResolver = mock(AuthenticationServiceResolver.class);
        when(authServiceResolver.getHost(any())).thenReturn("example.com");
        when(authServiceResolver.getPort(any())).thenReturn(5671);
        when(authServiceResolver.getCaSecretName(any())).thenReturn(Optional.of("certs"));
        resourceFactory = new TemplateInfraResourceFactory(
                new KubernetesHelper("test",
                        client,
                        client.getConfiguration().getOauthToken(),
                        new File("src/test/resources/templates"),
                        true),
                a -> authServiceResolver,
                true);
    }

    @Test
    public void testGenerateStandard() {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("myproject")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()

                .withNewSpec()
                .withType("standard")
                .withPlan("standard-unlimited")
                .addToEndpoints(new EndpointSpecBuilder()
                        .withName("messaging")
                        .withService("messaging")
                        .withCert(new CertSpec("selfsigned", "messaging-secret", null, null))
                        .build())
                .addToEndpoints(new EndpointSpecBuilder()
                        .withName("console")
                        .withService("console")
                        .withCert(new CertSpec("selfsigned", "console-secret", null, null))
                        .build())
                .endSpec()

                .build();

        StandardInfraConfig infraConfig = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("test")
                .endMetadata()

                .withNewSpec()
                .withVersion("master")
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources("2Mi")
                        .build())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withNewResources("2Mi", "1Gi")
                        .withAddressFullPolicy("FAIL")
                        .build())
                .withRouter(new StandardInfraConfigSpecRouterBuilder()
                        .withNewResources("2Mi")
                        .withLinkCapacity(22)
                        .build())
                .endSpec()
                .build();
        List<HasMetadata> items = resourceFactory.createInfraResources(addressSpace, infraConfig);
        assertEquals(1, items.size());
        ConfigMap map = findItem(ConfigMap.class, "ConfigMap", "mymap", items);
        assertEquals("FAIL", map.getData().get("key"));
    }

    private <T> T findItem(Class<T> clazz, String kind, String name, List<HasMetadata> items) {
        T found = null;
        for (HasMetadata item : items) {
            if (kind.equals(item.getKind()) && name.equals(item.getMetadata().getName())) {
                found = clazz.cast(item);
                break;
            }
        }
        assertNotNull(found);
        return found;
    }
}
