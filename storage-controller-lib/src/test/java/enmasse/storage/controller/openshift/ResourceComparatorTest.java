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

package enmasse.storage.controller.openshift;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceComparatorTest {
    @Test
    public void testSort() {
        List<IResource> resources = createResources(ResourceKind.POD, ResourceKind.CONFIG_MAP, ResourceKind.REPLICATION_CONTROLLER, ResourceKind.POD, ResourceKind.DEPLOYMENT_CONFIG, ResourceKind.REPLICATION_CONTROLLER);
        Collections.sort(resources, new ResourceComparator());
        assertThat(resources.size(), is(6));
        assertThat(resources.get(0).getKind(), is(ResourceKind.CONFIG_MAP));
        assertThat(resources.get(1).getKind(), is(ResourceKind.DEPLOYMENT_CONFIG));
        assertThat(resources.get(2).getKind(), is(ResourceKind.REPLICATION_CONTROLLER));
        assertThat(resources.get(3).getKind(), is(ResourceKind.REPLICATION_CONTROLLER));
        assertThat(resources.get(4).getKind(), is(ResourceKind.POD));
        assertThat(resources.get(5).getKind(), is(ResourceKind.POD));
    }

    private static List<IResource> createResources(String ... kinds) {
        List<IResource> resources = new ArrayList<>();
        for (String kind : kinds) {
            resources.add(createResource(kind));
        }
        return resources;
    }

    private static IResource createResource(String kind) {
        IResource resource = mock(IResource.class);
        when(resource.getKind()).thenReturn(kind);
        return resource;
    }
}
