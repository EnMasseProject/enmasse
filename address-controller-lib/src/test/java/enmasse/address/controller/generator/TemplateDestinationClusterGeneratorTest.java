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

package enmasse.address.controller.generator;

import enmasse.address.controller.admin.FlavorManager;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.Flavor;
import enmasse.address.controller.openshift.DestinationCluster;
import enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import io.fabric8.openshift.api.model.TemplateListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.ClientTemplateResource;
import io.fabric8.openshift.client.dsl.TemplateOperation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TemplateDestinationClusterGeneratorTest {
    private OpenShiftClient mockClient;
    private FlavorManager flavorManager = new FlavorManager();
    private DestinationClusterGenerator generator;

    @Before
    public void setUp() {
        mockClient = mock(OpenShiftClient.class);
        generator = new TemplateDestinationClusterGenerator(mockClient, flavorManager);
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder("vanilla", "test").build()));
    }

    @Test
    public void testDirect() {
        Destination dest = new Destination("foo.bar_baz.cockooA", false, false, Optional.empty());
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        DestinationCluster clusterList = generateCluster(dest, captor);
        Destination first = clusterList.getDestinationGroup().getDestinations().iterator().next();
        assertThat(first, is(dest));
        List<HasMetadata> resources = clusterList.getResources();
        assertThat(resources.size(), is(1));
        for (HasMetadata resource : resources) {
            Map<String, String> rlabel = resource.getMetadata().getLabels();
            assertNotNull(rlabel.get(LabelKeys.GROUP_ID));
            assertThat(rlabel.get(LabelKeys.GROUP_ID), is("foo-bar-baz-cockooa"));
            assertThat(rlabel.get(LabelKeys.ADDRESS_CONFIG), is("address-config-foo-bar-baz-cockooa"));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(3));
    }

    @Test
    public void testStoreAndForward() {
        Destination dest = new Destination("foo.bar", true, false, Optional.of("vanilla"));
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        DestinationCluster clusterList = generateCluster(dest, captor);
        assertThat(clusterList.getDestinationGroup().getDestinations(), hasItem(dest));
        List<HasMetadata> resources = clusterList.getResources();
        assertThat(resources.size(), is(1));
        for (HasMetadata resource : resources) {
            Map<String, String> rlabel = resource.getMetadata().getLabels();
            assertNotNull(rlabel.get(LabelKeys.GROUP_ID));
            assertThat(rlabel.get(LabelKeys.GROUP_ID), is("foo-bar"));
            assertThat(rlabel.get(LabelKeys.ADDRESS_CONFIG), is("address-config-foo-bar"));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(2));
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
        ClientTemplateResource templateResource = mock(ClientTemplateResource.class);
        when(templateOp.list()).thenReturn(new TemplateListBuilder().addToItems(template).build());
        when(templateOp.withName(anyString())).thenReturn(templateResource);
        when(templateResource.get()).thenReturn(template);
        when(templateResource.process(captor.capture())).thenReturn(new KubernetesListBuilder().addNewConfigMapItem().withNewMetadata().withName("testmap").endMetadata().endConfigMapItem().build());
        when(mockClient.templates()).thenReturn(templateOp);

        return generator.generateCluster(new DestinationGroup(destination.address(), Collections.singleton(destination)));
    }

}
