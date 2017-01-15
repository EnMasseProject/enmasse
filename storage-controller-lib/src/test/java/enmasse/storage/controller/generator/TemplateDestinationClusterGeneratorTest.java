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

package enmasse.storage.controller.generator;

import enmasse.storage.controller.admin.FlavorManager;
import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.Flavor;
import enmasse.storage.controller.model.LabelKeys;
import enmasse.storage.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.api.model.*;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.ClientTemplateResource;
import io.fabric8.openshift.client.dsl.TemplateOperation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
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
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder().templateName("test").build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddressTypeRequired() {
        Destination dest = new Destination("foo", true, false, "vanilla");
        Template template = new TemplateBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName("test")
                    .addToLabels("key1", "value1")
                    .build())
                .build();

        TemplateOperation templateOp = mock(TemplateOperation.class);
        ClientTemplateResource templateResource = mock(ClientTemplateResource.class);
        when(templateOp.list()).thenReturn(new TemplateListBuilder().addToItems(template).build());
        when(templateOp.withName(anyString())).thenReturn(templateResource);
        when(templateResource.get()).thenReturn(template);
        when(mockClient.templates()).thenReturn(templateOp);
        generator.generateCluster(dest);
    }

    @Test
    public void testDirect() {
        Destination dest = new Destination("foo.bar_baz.cockooA", false, false, "");
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        DestinationCluster clusterList = generateCluster(dest, captor);
        assertThat(clusterList.getDestination(), is(dest));
        List<HasMetadata> resources = clusterList.getResources();
        assertThat(resources.size(), is(1));
        for (HasMetadata resource : resources) {
            Map<String, String> rlabel = resource.getMetadata().getLabels();
            assertThat(rlabel.get(LabelKeys.ADDRESS), is(dest.address()));
            assertThat(rlabel.get(LabelKeys.FLAVOR), is(dest.flavor()));
            assertThat(rlabel.get(LabelKeys.ADDRESS_TYPE), is(AddressType.QUEUE.value()));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(3));
    }

    @Test
    public void testStoreAndForward() {
        Destination dest = new Destination("foo.bar", true, false, "vanilla");
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        DestinationCluster clusterList = generateCluster(dest, captor);
        assertThat(clusterList.getDestination(), is(dest));
        List<HasMetadata> resources = clusterList.getResources();
        assertThat(resources.size(), is(1));
        for (HasMetadata resource : resources) {
            Map<String, String> rlabel = resource.getMetadata().getLabels();
            assertThat(rlabel.get(LabelKeys.ADDRESS), is(dest.address()));
            assertThat(rlabel.get(LabelKeys.FLAVOR), is(dest.flavor()));
            assertThat(rlabel.get(LabelKeys.ADDRESS_TYPE), is(AddressType.QUEUE.value()));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(3));
    }

    private DestinationCluster generateCluster(Destination destination, ArgumentCaptor<ParameterValue> captor) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.ADDRESS_TYPE, AddressType.QUEUE.value());
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

        return generator.generateCluster(destination);
    }

}
