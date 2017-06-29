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

import enmasse.config.AnnotationKeys;
import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Flavor;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import enmasse.controller.address.DestinationCluster;
import enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.TemplateResource;
import io.fabric8.openshift.client.dsl.TemplateOperation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.*;

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
        generator = new TemplateDestinationClusterGenerator(new Instance.Builder(InstanceId.withId("myinstance")).build(), new KubernetesHelper(InstanceId.withId("myinstance"), mockClient, Optional.of(new File("src/test/resources/templates"))), flavorManager);
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder("vanilla", "test").build()));
    }

    @Test
    public void testDirect() {
        Destination dest = new Destination("foo.bar_baz.cockooA", "foo.bar_baz.cockooA", false, false, Optional.empty(), Optional.empty(), new Destination.Status(false));
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        DestinationCluster clusterList = generateCluster(dest, captor);
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(0));
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testStoreAndForward() {
        Destination dest = new Destination("foo.bar", "foo.bar", true, false, Optional.of("vanilla"), Optional.empty(), new Destination.Status(false));
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        DestinationCluster clusterList = generateCluster(dest, captor);
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(1));
        for (HasMetadata resource : resources) {
            Map<String, String> annotations = resource.getMetadata().getAnnotations();
            assertNotNull(annotations.get(AnnotationKeys.GROUP_ID));
            assertThat(annotations.get(AnnotationKeys.GROUP_ID), is("foo.bar"));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(5));
    }

    private DestinationCluster generateCluster(Destination destination, ArgumentCaptor<ParameterValue> captor) {
        TemplateOperation templateOp = mock(TemplateOperation.class);
        TemplateResource templateResource = mock(TemplateResource.class);
        when(templateOp.load(any(File.class))).thenReturn(templateResource);
        when(templateResource.processLocally(captor.capture())).thenReturn(new KubernetesListBuilder().addNewConfigMapItem().withNewMetadata().withName("testmap").endMetadata().endConfigMapItem().build());
        when(mockClient.templates()).thenReturn(templateOp);

        return generator.generateCluster(Collections.singleton(destination));
    }

}
