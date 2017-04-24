/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.controller.common;

import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Flavor;
import enmasse.controller.model.InstanceId;
import enmasse.controller.address.DestinationCluster;
import enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.TemplateResource;
import io.fabric8.openshift.client.dsl.TemplateOperation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TemplateDestinationClusterGeneratorTest {
    private OpenShiftClient mockClient;
    private FlavorManager flavorManager = new FlavorManager();
    private DestinationClusterGenerator generator;

    @Before
    public void setUp() {
        mockClient = mock(OpenShiftClient.class);
        generator = new TemplateDestinationClusterGenerator(InstanceId.withId("myinstance"), new KubernetesHelper(InstanceId.withId("myinstance"), mockClient, new File("src/test/resources/templates")), flavorManager);
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder("vanilla", "test").build()));
    }

    @Test
    public void testDirect() {
        Destination dest = new Destination("foo.bar_baz.cockooA", "foo.bar_baz.cockooA", false, false, Optional.empty(), Optional.empty());
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        DestinationCluster clusterList = generateCluster(dest, captor);
        Destination first = clusterList.getDestinations().iterator().next();
        assertThat(first, is(dest));
        List<HasMetadata> resources = clusterList.getResources();
        assertThat(resources.size(), is(1));
        assertTrue(resources.get(0) instanceof ConfigMap);
        for (HasMetadata resource : resources) {
            ConfigMap map = (ConfigMap) resource;
            Map<String, String> rlabel = resource.getMetadata().getLabels();
            assertNotNull(rlabel.get(LabelKeys.GROUP_ID));
            assertThat(rlabel.get(LabelKeys.GROUP_ID), is("foo-bar-baz-cockooa"));
            assertThat(rlabel.get(LabelKeys.ADDRESS_CONFIG), is("address-config-myinstance-foo-bar-baz-cockooa"));
            assertThat(map.getData().size(), is(1));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testStoreAndForward() {
        Destination dest = new Destination("foo.bar", "foo.bar", true, false, Optional.of("vanilla"), Optional.empty());
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        DestinationCluster clusterList = generateCluster(dest, captor);
        assertThat(clusterList.getDestinations(), hasItem(dest));
        List<HasMetadata> resources = clusterList.getResources();
        assertThat(resources.size(), is(2));
        for (HasMetadata resource : resources) {
            Map<String, String> rlabel = resource.getMetadata().getLabels();
            assertNotNull(rlabel.get(LabelKeys.GROUP_ID));
            assertThat(rlabel.get(LabelKeys.GROUP_ID), is("foo-bar"));
            assertThat(rlabel.get(LabelKeys.ADDRESS_CONFIG), is("address-config-myinstance-foo-bar"));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(3));
    }

    private DestinationCluster generateCluster(Destination destination, ArgumentCaptor<ParameterValue> captor) {
        Map<String, String> labels = new LinkedHashMap<>();
        Template template = new TemplateBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName("vanilla")
                    .withLabels(labels)
                    .build())
                .build();

        TemplateOperation templateOp = mock(TemplateOperation.class);
        TemplateResource templateResource = mock(TemplateResource.class);
        when(templateOp.load(any(File.class))).thenReturn(templateResource);
        when(templateResource.processLocally(captor.capture())).thenReturn(new KubernetesListBuilder().addNewConfigMapItem().withNewMetadata().withName("testmap").endMetadata().endConfigMapItem().build());
        when(mockClient.templates()).thenReturn(templateOp);

        return generator.generateCluster(Collections.singleton(destination));
    }

}
