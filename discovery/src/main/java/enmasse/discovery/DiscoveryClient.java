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

package enmasse.discovery;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ulf Lilleengen
 */
public class DiscoveryClient implements IOpenShiftWatchListener {
    private final IClient osClient;
    private final String namespace;
    private final Map<String, String> labelFilter;
    private final List<DiscoveryListener> listeners = new ArrayList<>();
    private final Logger log = LoggerFactory.getLogger(DiscoveryClient.class.getName());
    private volatile IWatcher watcher;

    public DiscoveryClient(IClient osClient, String namespace, Map<String, String> labelFilter) {
        this.osClient = osClient;
        this.namespace = namespace;
        this.labelFilter = labelFilter;
    }

    public void addListener(DiscoveryListener listener) {
        this.listeners.add(listener);
    }

    public void start() {
        watcher = osClient.watch(namespace, this, ResourceKind.POD);
    }

    public void stop() {
        if (watcher != null) {
            watcher.stop();
        }
    }

    @Override
    public void connected(List<IResource> resources) {
        Set<Host> hosts = resources.stream()
                .filter(r -> filterLabels(r.getLabels()))
                .map(r -> podToHost((IPod)r))
                .filter(host -> !host.getHostname().isEmpty())
                .collect(Collectors.toSet());

        log.debug("Connected with " + hosts.size() + " hosts");
        notifyListeners(hosts);
    }

    private boolean filterLabels(Map<String, String> labels) {
        for (Map.Entry<String, String> entrySet : labelFilter.entrySet()) {
            if (!labels.containsKey(entrySet.getKey()) || !labels.get(entrySet.getKey()).equals(entrySet.getValue())) {
                return false;
            }
        }
        return true;
    }

    private void notifyListeners(Set<Host> hosts) {
        for (DiscoveryListener listener : listeners) {
            listener.hostsChanged(hosts);
        }
    }

    private static final Host podToHost(IPod pod) {
        IContainer broker = findContainer(pod, "broker");
        return new Host(pod.getIP(), createPortMap(broker.getPorts()));
    }

    private static IContainer findContainer(IPod pod, String name) {
        for (IContainer container : pod.getContainers()) {
            if (name.equals(container.getName())) {
                return container;
            }
        }
        throw new IllegalArgumentException("Unable to find container with name " + name);
    }

    private static Map<String, Integer> createPortMap(Set<IPort> containerPorts) {
        Map<String, Integer> portMap = new LinkedHashMap<>();
        for (IPort port : containerPorts) {
            if (port.getName() == null) {
                portMap.put(String.valueOf(port.getContainerPort()), port.getContainerPort());
            } else {
                portMap.put(port.getName(), port.getContainerPort());
            }
        }
        return portMap;
    }

    @Override
    public void disconnected() {
        log.debug("Disconnected, reconnecting");
        watcher = osClient.watch(namespace, this, ResourceKind.POD);
    }

    private final Set<Host> fetchHosts() {
        return osClient.<IPod>list(ResourceKind.POD, namespace, labelFilter).stream()
                .map(DiscoveryClient::podToHost)
                .filter(host -> !host.getHostname().isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public void received(IResource resource, ChangeType change) {
        log.debug("Recieved change for " + resource.getName());
        if (filterLabels(resource.getLabels())) {
            notifyListeners(fetchHosts());
        }
    }

    @Override
    public void error(Throwable err) {
        log.error("Got error: " + err.getMessage());
    }
}
