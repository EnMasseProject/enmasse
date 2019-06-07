/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class TemplateBrokerSetGeneratorTest {
    public static final String CONTAINER_NAME = "container1";
    private Kubernetes kubernetes;
    private StandardControllerSchema standardControllerSchema;
    private BrokerSetGenerator generator;

    @BeforeEach
    public void setUp() {
        kubernetes = mock(Kubernetes.class);

        standardControllerSchema = new StandardControllerSchema();
        generator = new TemplateBrokerSetGenerator(kubernetes, new StandardControllerOptions(), Collections.emptyMap());
    }

    @Test
    public void testDirect() throws Exception {
        Address dest = createAddress("foo_bar_FOO", "anycast");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String,String>> captor = ArgumentCaptor.forClass(Map.class);
        BrokerCluster clusterList = generateCluster(dest, captor);
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(1));
        Map<String,String> parameters = captor.getValue();
        assertThat(parameters.size(), is(13));
    }

    @Test
    public void testStoreAndForward() throws Exception {
        Address dest = createAddress("foo.bar", "queue");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String,String>> captor = ArgumentCaptor.forClass(Map.class);
        BrokerCluster clusterList = generateCluster(dest, captor);
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(1));
        assertResourcesHaveClusterId(dest.getMetadata().getName(), resources);
        StatefulSet set = (StatefulSet) resources.get(0);
        assertThat(set.getSpec().getVolumeClaimTemplates().get(0).getSpec().getStorageClassName(), is("mysc"));
        assertThat(set.getSpec().getReplicas(), is(1));

        PodTemplateSpec templateSpec = standardControllerSchema.getSchema().findAddressSpaceType("standard").map(type -> (StandardInfraConfig) type.findInfraConfig("cfg1").orElse(null)).orElse(null).getSpec().getBroker().getPodTemplate();
        assertTemplateSpec(set.getSpec().getTemplate(), templateSpec);
        Map<String,String> parameters = captor.getValue();
        assertThat(parameters.size(), is(13));
    }

    @Test
    public void testContainerEnv() throws Exception {
        EnvVar myEnv = new EnvVarBuilder().withName("MYVAR1").withValue("NEWVALUE").build();

        PodTemplateSpec templateSpec = standardControllerSchema.getSchema().findAddressSpaceType("standard").map(type -> (StandardInfraConfig) type.findInfraConfig("cfg1").orElse(null)).orElse(null).getSpec().getBroker().getPodTemplate();
        templateSpec.getSpec().setContainers(Collections.singletonList(new ContainerBuilder()
                .withName(CONTAINER_NAME)
                .withEnv(myEnv)
                .build()));

        Address dest = createAddress("foo.bar", "queue");
        @SuppressWarnings("unchecked")
        BrokerCluster clusterList = generateCluster(dest, ArgumentCaptor.<Map<String, String>, Map>forClass(Map.class));
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(1));
        assertResourcesHaveClusterId(dest.getMetadata().getName(), resources);
        StatefulSet set = (StatefulSet) resources.get(0);

        StatefulSetSpec spec = set.getSpec();
        List<Container> containers = spec.getTemplate().getSpec().getContainers();
        assertThat(containers.size(), is(1));
        List<EnvVar> envVars = containers.get(0).getEnv();

        assertThat(envVars.size(), is(1));
        assertThat(envVars, hasItem(myEnv));
    }

    @Test
    public void testSupplementedContainerEnv() throws Exception {
        EnvVar original = new EnvVarBuilder().withName("MYVAR1").withValue("ORIGINAL").build();
        EnvVar replacement = new EnvVarBuilder().withName("MYVAR1").withValue("NEWVALUE").build();
        EnvVar other = new EnvVarBuilder().withName("OTHER").withValue("OTHERVAL").build();

        PodTemplateSpec templateSpec = standardControllerSchema.getSchema().findAddressSpaceType("standard").map(type -> (StandardInfraConfig) type.findInfraConfig("cfg1").orElse(null)).orElse(null).getSpec().getBroker().getPodTemplate();
        templateSpec.getSpec().setContainers(Collections.singletonList(new ContainerBuilder()
                .withName(CONTAINER_NAME)
                .withEnv(replacement)
                .build()));

        Address dest = createAddress("foo.bar", "queue");
        @SuppressWarnings("unchecked")
        BrokerCluster clusterList = generateCluster(dest, ArgumentCaptor.<Map<String, String>, Map>forClass(Map.class), Arrays.asList(original, other));
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(1));
        assertResourcesHaveClusterId(dest.getMetadata().getName(), resources);
        StatefulSet set = (StatefulSet) resources.get(0);

        StatefulSetSpec spec = set.getSpec();
        List<Container> containers = spec.getTemplate().getSpec().getContainers();
        List<EnvVar> envVars = containers.get(0).getEnv();

        assertThat(envVars.size(), is(2));
        assertThat(envVars, hasItems(replacement, other));
    }

    private void assertResourcesHaveClusterId(String expectedClusterId, List<HasMetadata> resources) {
        for (HasMetadata resource : resources) {
            Map<String, String> annotations = resource.getMetadata().getAnnotations();
            assertNotNull(annotations.get(AnnotationKeys.CLUSTER_ID));
            assertThat(annotations.get(AnnotationKeys.CLUSTER_ID), is(expectedClusterId));
        }
    }

    private Address createAddress(String address, String type) {
        return new AddressBuilder()
                .withNewMetadata()
                .withName(address)
                .endMetadata()

                .withNewSpec()
                .withAddress(address)
                .withAddressSpace("myinstance")
                .withType(type)
                .withPlan("plan1")
                .endSpec()

                .build();
    }

    private BrokerCluster generateCluster(Address address, ArgumentCaptor<Map<String, String>> captor) throws Exception {
        return generateCluster(address, captor, Collections.emptyList());
    }

    private BrokerCluster generateCluster(Address address, ArgumentCaptor<Map<String, String>> captor, List<EnvVar> envVars) throws Exception {
        when(kubernetes.processTemplate(anyString(), captor.capture())).thenReturn(new KubernetesListBuilder().addNewStatefulSetItem().withNewMetadata().withName("testset").endMetadata().
                withNewSpec()
                .withReplicas(0)
                .withNewTemplate()
                .editOrNewMetadata()
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(CONTAINER_NAME)
                .withEnv(envVars)
                .endContainer()
                .endSpec()
                .endTemplate()
                .withVolumeClaimTemplates(new PersistentVolumeClaimBuilder()
                        .withNewSpec()
                        .endSpec()
                        .build())
                .endSpec()
                .endStatefulSetItem().build());

        return generator.generateCluster(address.getMetadata().getName(), 1, address, null,
                standardControllerSchema.getSchema().findAddressSpaceType("standard").map(type -> (StandardInfraConfig) type.findInfraConfig("cfg1").orElse(null)).orElse(null));
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
}
