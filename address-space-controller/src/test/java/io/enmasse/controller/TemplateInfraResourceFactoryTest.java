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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.enmasse.admin.model.v1.*;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.EndpointSpecBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.KubernetesHelper;
import io.enmasse.k8s.util.JULInitializingTest;

public class TemplateInfraResourceFactoryTest extends JULInitializingTest {

    private KubernetesServer kubeServer = new KubernetesServer(false, true);

    private TemplateInfraResourceFactory resourceFactory;
    private NamespacedKubernetesClient client;

    @AfterEach
    void tearDown() {
        kubeServer.after();
    }

    @BeforeEach
    public void setup() {
        kubeServer.before();
        client = kubeServer.getClient();
        client.secrets().createNew().editOrNewMetadata().withName("certs").endMetadata().addToData("tls.crt", "cert").done();
        AuthenticationServiceRegistry authenticationServiceRegistry = mock(AuthenticationServiceRegistry.class);
        AuthenticationService authenticationService = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName("standard")
                .endMetadata()
                .withNewSpec()
                .withType(AuthenticationServiceType.none)
                .endSpec()
                .withNewStatus()
                .withHost("example")
                .withPort(5671)
                .withCaCertSecret(new SecretReferenceBuilder().withName("certs").build())
                .endStatus()
                .build();
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(authenticationService));

        resourceFactory = new TemplateInfraResourceFactory(
                new KubernetesHelper("test",
                        client,
                        new File("src/test/resources/templates"),
                        true),
                authenticationServiceRegistry,
                Collections.emptyMap(), false);
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

        PodTemplateSpec routerTemplateSpec = createTemplateSpec(Collections.singletonMap("mylabel", "router"), "myrnode", "myrkey", "myrClass");
        PodTemplateSpec adminTemplateSpec = createTemplateSpec(Collections.singletonMap("mylabel", "broker"), "mybnode", "mybkey", "mybClass");
        StandardInfraConfig infraConfig = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("test")
                .endMetadata()

                .withNewSpec()
                .withVersion("master")
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources("2Mi")
                        .withPodTemplate(adminTemplateSpec)
                        .build())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withNewResources("2Mi", "1Gi")
                        .withAddressFullPolicy("FAIL")
                        .build())
                .withRouter(new StandardInfraConfigSpecRouterBuilder()
                        .withNewResources("2Mi")
                        .withLinkCapacity(22)
                        .withPodTemplate(routerTemplateSpec)
                        .build())
                .endSpec()
                .build();
        List<HasMetadata> items = resourceFactory.createInfraResources(addressSpace, infraConfig);
        assertEquals(3, items.size());
        ConfigMap map = findItem(ConfigMap.class, "ConfigMap", "mymap", items);
        assertEquals("FAIL", map.getData().get("key"));

        StatefulSet routerSet = findItem(StatefulSet.class, "StatefulSet", "qdrouterd-1234", items);
        assertTemplateSpec(routerSet.getSpec().getTemplate(), routerTemplateSpec);

        Deployment adminDeployment = findItem(Deployment.class, "Deployment", "admin.1234", items);
        assertTemplateSpec(adminDeployment.getSpec().getTemplate(), adminTemplateSpec);
    }

    public static PodTemplateSpec createTemplateSpec(Map<String, String> labels, String nodeAffinityValue, String tolerationKey, String priorityClassName) {
        PodTemplateSpecBuilder builder = new PodTemplateSpecBuilder();
        if (labels != null) {
            builder.editOrNewMetadata()
                    .withLabels(labels)
                    .endMetadata();
        }

        if (nodeAffinityValue != null) {
            builder.editOrNewSpec()
                    .editOrNewAffinity()
                    .editOrNewNodeAffinity()
                    .addToPreferredDuringSchedulingIgnoredDuringExecution(new PreferredSchedulingTermBuilder()
                            .withNewPreference()
                            .addToMatchExpressions(new NodeSelectorRequirementBuilder()
                                    .addToValues(nodeAffinityValue)
                                    .build())
                            .endPreference()
                            .build())
                    .endNodeAffinity()
                    .endAffinity()
                    .endSpec();
        }

        if (tolerationKey != null) {
            builder.editOrNewSpec()
                    .addNewToleration()
                    .withKey(tolerationKey)
                    .withOperator("Exists")
                    .withEffect("NoSchedule")
                    .endToleration()
                    .endSpec();
        }

        if (priorityClassName != null) {
            builder.editOrNewSpec()
                    .withPriorityClassName(priorityClassName)
                    .endSpec();
        }

        return builder.build();
    }


    private void assertTemplateSpec(PodTemplateSpec pod, PodTemplateSpec templateSpec) {
        if (templateSpec.getMetadata().getLabels() != null) {
            for (Map.Entry<String, String> labelPair : templateSpec.getMetadata().getLabels().entrySet()) {
                assertEquals(labelPair.getValue(), pod.getMetadata().getLabels().get(labelPair.getKey()), "Labels do not match");
            }
        }

        if (templateSpec.getSpec().getAffinity() != null) {
            assertEquals(templateSpec.getSpec().getAffinity(), pod.getSpec().getAffinity(), "Affinity rules do not match");
        }

        if (templateSpec.getSpec().getPriorityClassName() != null) {
            assertEquals(templateSpec.getSpec().getPriorityClassName(), pod.getSpec().getPriorityClassName(), "Priority class names do not match");
        }

        if (templateSpec.getSpec().getTolerations() != null) {
            assertEquals(templateSpec.getSpec().getTolerations(), pod.getSpec().getTolerations(), "List of tolerations does not match");
        }

        for (Container expectedContainer : templateSpec.getSpec().getContainers()) {
            for (Container actualContainer : pod.getSpec().getContainers()) {
                if (expectedContainer.getName().equals(actualContainer.getName())) {
                    assertEquals(expectedContainer.getResources(), actualContainer.getResources());
                }
            }
        }
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
