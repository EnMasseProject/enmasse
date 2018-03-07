/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.discovery;

import io.enmasse.k8s.api.Watcher;
import io.enmasse.k8s.api.cache.*;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DiscoveryClient implements ListerWatcher<io.fabric8.kubernetes.api.model.Pod, PodList> {
    private final List<DiscoveryListener> listeners = new ArrayList<>();
    private final Logger log = LoggerFactory.getLogger(DiscoveryClient.class.getName());
    private final String containerName;
    private final Map<String, String> labelFilter;
    private final Map<String, String> annotationFilter;
    private Set<Host> currentHosts = new LinkedHashSet<>();
    private final KubernetesClient client;
    private final Controller controller;

    public DiscoveryClient(KubernetesClient client, Map<String, String> labelFilter, Map<String, String> annotationFilter, String containerName) {
        this.client = client;
        this.labelFilter = labelFilter;
        this.annotationFilter = annotationFilter;
        this.containerName = containerName;
        WorkQueue<io.fabric8.kubernetes.api.model.Pod> queue = new FifoQueue<>(pod -> pod.getMetadata().getName());
        Reflector.Config<io.fabric8.kubernetes.api.model.Pod, PodList> config = new Reflector.Config<>();
        config.setClock(Clock.systemUTC());
        config.setExpectedType(io.fabric8.kubernetes.api.model.Pod.class);
        config.setListerWatcher(this);
        config.setResyncInterval(Duration.ofMinutes(5));
        config.setWorkQueue(queue);
        config.setProcessor(map -> {
            if (queue.hasSynced()) {
                resourcesUpdated(queue.list().stream()
                        .map(Pod::new)
                        .filter(this::filterPod)
                        .collect(Collectors.toList()));
            }
        });

        Reflector<io.fabric8.kubernetes.api.model.Pod, PodList> reflector = new Reflector<>(config);
        controller = new Controller(reflector);
    }

    public DiscoveryClient(Map<String, String> labelFilter, Map<String, String> annotationFilter, String containerName) {
        this(new DefaultKubernetesClient(), labelFilter, annotationFilter, containerName);
    }

    public void addListener(DiscoveryListener listener) {
        this.listeners.add(listener);
    }

    private void notifyListeners(Set<Host> hosts) {
        if (currentHosts.equals(hosts)) {
            return;
        }
        currentHosts = new LinkedHashSet<>(hosts);
        log.info("Received new set of hosts: " + hosts);
        for (DiscoveryListener listener : listeners) {
            listener.hostsChanged(hosts);
        }
    }

    public void start() {
        controller.start();
    }

    void resourcesUpdated(List<Pod> resources) {
        Set<Host> hosts = new HashSet<>();
        for (Pod pod : resources) {

            String host = pod.getHost();
            String ready = pod.getReady();
            String phase = pod.getPhase();
            if ("True".equals(ready) && "Running".equals(phase)) {
                Map<String, Map<String, Integer>> portMap = pod.getPortMap();
                if (containerName != null) {
                    hosts.add(new Host(host, portMap.get(containerName)));
                } else {
                    hosts.add(new Host(host, portMap.values().iterator().next()));
                }
            }
        }

        notifyListeners(hosts);
    }

    public void stop() throws InterruptedException {
        controller.stop();
    }

    private boolean filterPod(Pod pod) {
        Map<String, String> annotations = pod.getAnnotations();
        if (annotationFilter.isEmpty()) {
            return true;
        }

        if (annotations == null) {
            return false;
        }

        for (Map.Entry<String, String> filterEntry : annotationFilter.entrySet()) {
            String annotationValue = annotations.get(filterEntry.getKey());
            if (annotationValue == null || !annotationValue.equals(filterEntry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public PodList list(ListOptions listOptions) {
        return client.pods().withLabels(labelFilter).list();
    }

    @Override
    public Watch watch(io.fabric8.kubernetes.client.Watcher<io.fabric8.kubernetes.api.model.Pod> watcher, ListOptions listOptions) {
        return client.pods().withLabels(labelFilter).withResourceVersion(listOptions.getResourceVersion()).watch(watcher);
    }
}
