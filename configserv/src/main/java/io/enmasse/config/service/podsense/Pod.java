/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.podsense;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.PodCondition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a podsense resource.
 */
public class Pod {
    private final String name;
    private final String kind;
    private final String host;
    private final String ready;
    private final String phase;
    private final Map<String, Map<String, Integer>> portMap;
    private final Map<String, String> annotations = new LinkedHashMap<>();

    public Pod(io.fabric8.kubernetes.api.model.Pod pod) {
        this.name = pod.getMetadata().getName();
        if (pod.getMetadata().getAnnotations() != null) {
            this.annotations.putAll(pod.getMetadata().getAnnotations());
        }
        this.kind = pod.getKind();
        this.host = pod.getStatus().getPodIP();
        this.phase = pod.getStatus().getPhase();
        this.ready = getReadyCondition(pod.getStatus().getConditions());
        this.portMap = getPortMap(pod.getSpec().getContainers());
    }

    private String getReadyCondition(List<PodCondition> conditions) {
        for (PodCondition condition : conditions) {
            if ("Ready".equals(condition.getType())) {
                return condition.getStatus();
            }
        }
        return "Unknown";
    }

    private static Map<String, Map<String, Integer>> getPortMap(List<Container> containers) {
        Map<String, Map<String, Integer>> portMap = new LinkedHashMap<>();
        for (Container container : containers) {
            Map<String, Integer> ports = new LinkedHashMap<>();
            for (ContainerPort port : container.getPorts()) {
                ports.put(port.getName(), port.getContainerPort());
            }
            portMap.put(container.getName(), ports);
        }
        return portMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pod that = (Pod) o;

        if (!name.equals(that.name)) return false;
        if (!kind.equals(that.kind)) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (phase != null ? !phase.equals(that.phase) : that.phase != null) return false;
        if (ready != null ? !ready.equals(that.ready) : that.ready != null) return false;
        return portMap != null ? portMap.equals(that.portMap) : that.portMap == null;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("kind=").append(kind);
        builder.append(", name=").append(name);
        builder.append(", host=").append(host);
        builder.append(", phase=").append(phase);
        builder.append(", ready=").append(ready);
        return builder.toString();
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + kind.hashCode();
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (phase != null ? phase.hashCode() : 0);
        result = 31 * result + (ready != null ? ready.hashCode() : 0);
        result = 31 * result + (portMap != null ? portMap.hashCode() : 0);
        return result;
    }

    public String getPhase() {
        return phase;
    }

    public String getReady() {
        return ready;
    }

    public String getHost() {
        return host;
    }

    public Map<String, Map<String, Integer>> getPortMap() {
        return portMap;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }
}
