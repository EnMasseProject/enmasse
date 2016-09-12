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

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.internal.restclient.capability.server.ServerTemplateProcessing;
import com.openshift.restclient.IClient;
import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.UnsupportedOperationException;
import com.openshift.restclient.capability.server.ITemplateProcessing;
import com.openshift.restclient.model.IList;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.ITemplate;
import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.LabelKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A wrapper for dealing with openshift-restclient.
 */
public class OpenshiftClient {
    private static final Logger log = LoggerFactory.getLogger(OpenshiftClient.class.getName());
    private final IClient client;
    private final String namespace;
    private final ITemplateProcessing templateProcessor;
    private final IResourceFactory resourceFactory;

    public OpenshiftClient(IClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
        this.resourceFactory = new ResourceFactory(client);
        this.templateProcessor = new ServerTemplateProcessing(client);
    }

    public void createResources(Collection<IResource> resources) {
        log.info("Adding " + resources.size() + " resources");
        IList list = createList();
        list.addAll(resources);
        client.create(list, namespace);
    }

    public void deleteResources(Collection<IResource> resources) {
        log.info("Deleting " + resources.size() + " resources");
        for (IResource resource : resources) {
            client.delete(resource);
        }
    }

    public void updateResource(IResource resource) {
        log.info("Updating " + resource.getName());
        client.update(resource);
    }

    public ITemplate getTemplate(String name) {
        return client.get(ResourceKind.TEMPLATE, name, namespace);
    }

    public Collection<IResource> processTemplate(ITemplate template) {
        return templateProcessor.process(template, namespace).getObjects();
    }

    public List<StorageCluster> listClusters() {
        Map<Destination, List<IResource>> resourceMap = new HashMap<>();

        for (String kind : ResourceKind.values()) {
            List<IResource> kindResources = listAndIgnore(kind, namespace);
            for (IResource resource : kindResources) {
                Map<String, String> labels = resource.getLabels();
                if (labels.containsKey(LabelKeys.ADDRESS)) {
                    String address = labels.get(LabelKeys.ADDRESS);
                    String type = labels.get(LabelKeys.ADDRESS_TYPE);
                    String flavor = labels.get(LabelKeys.FLAVOR);
                    Destination destination = new Destination(address, true, AddressType.TOPIC.name().equals(type), flavor);
                    if (!resourceMap.containsKey(destination)) {
                        resourceMap.put(destination, new ArrayList<>());
                    }
                    resourceMap.get(destination).add(resource);
                }
            }
        }

        return resourceMap.entrySet().stream()
                .map(entry -> new StorageCluster(this, entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    private IList createList() {
        return resourceFactory.create("v1", ResourceKind.LIST);
    }

    private List<IResource> listAndIgnore(String kind, String namespace) {
        try {
            return client.list(kind, namespace);
        } catch (UnsupportedOperationException | OpenShiftException e) {
            // Ignore
            return Collections.emptyList();
        }
    }

    public IClient getClient() {
        return client;
    }
}

