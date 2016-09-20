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

import com.openshift.restclient.model.template.ITemplate;
import enmasse.storage.controller.admin.FlavorManager;
import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.Flavor;
import enmasse.storage.controller.model.LabelKeys;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TemplateStorageGeneratorTest {
    private OpenshiftClient mockClient;
    private FlavorManager flavorManager = new FlavorManager();
    private StorageGenerator generator;

    @Before
    public void setUp() {
        mockClient = mock(OpenshiftClient.class);
        generator = new TemplateStorageGenerator(mockClient, flavorManager);
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder().templateName("test").build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddressTypeRequired() {
        Destination dest = new Destination("foo", true, false, "vanilla");
        ITemplate template = mock(ITemplate.class);
        when(template.getName()).thenReturn("test");
        when(template.getLabels()).thenReturn(Collections.emptyMap());
        when(mockClient.getTemplate("test")).thenReturn(template);

        ArgumentCaptor<ITemplate> arg = ArgumentCaptor.forClass(ITemplate.class);
        when(mockClient.processTemplate(arg.capture())).thenReturn(Collections.emptyList());

        generator.generateStorage(dest);
    }

    @Test
    public void testDirect() {
        Destination dest = new Destination("foo", false, false, "");
        Map<String, String> labels = new LinkedHashMap<>();
        ITemplate template = mock(ITemplate.class);
        when(template.getName()).thenReturn("direct");
        when(template.getLabels()).thenReturn(labels);
        when(mockClient.getTemplate("direct")).thenReturn(template);

        ArgumentCaptor<ITemplate> arg = ArgumentCaptor.forClass(ITemplate.class);
        when(mockClient.processTemplate(arg.capture())).thenReturn(Collections.emptyList());

        StorageCluster clusterList = generator.generateStorage(dest);
        assertThat(clusterList.getDestination(), is(dest));
        verify(template).addObjectLabel(LabelKeys.ADDRESS, dest.address());
        verify(template).addObjectLabel(LabelKeys.FLAVOR, dest.flavor());
        verify(template).addObjectLabel(LabelKeys.ADDRESS_TYPE, AddressType.QUEUE.value());

    }

    @Test
    public void testStoreAndForward() {
        Destination dest = new Destination("foo", true, false, "vanilla");
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.ADDRESS_TYPE, AddressType.QUEUE.value());
        ITemplate template = mock(ITemplate.class);
        when(template.getName()).thenReturn("test");
        when(template.getLabels()).thenReturn(labels);
        when(mockClient.getTemplate("test")).thenReturn(template);

        ArgumentCaptor<ITemplate> arg = ArgumentCaptor.forClass(ITemplate.class);
        when(mockClient.processTemplate(arg.capture())).thenReturn(Collections.emptyList());

        StorageCluster clusterList = generator.generateStorage(dest);
        assertThat(clusterList.getDestination(), is(dest));
        verify(template).addObjectLabel(LabelKeys.ADDRESS, dest.address());
        verify(template).addObjectLabel(LabelKeys.FLAVOR, dest.flavor());
        verify(template).addObjectLabel(LabelKeys.ADDRESS_TYPE, AddressType.QUEUE.value());
    }
}
