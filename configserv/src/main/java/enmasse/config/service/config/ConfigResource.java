package enmasse.config.service.config;

import enmasse.config.LabelKeys;
import enmasse.config.service.model.Resource;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.Map;

/**
 * Extends a Kubernetes resource with metadata and applies hashCode and equals
 */
public class ConfigResource extends Resource {
    private final String kind;
    private final String name;
    private final Map<String, String> labels;
    private final Map<String, String> data;

    public ConfigResource(ConfigMap configMap) {
        this.kind = configMap.getKind();
        this.name = configMap.getMetadata().getName();
        this.labels = configMap.getMetadata().getLabels();
        this.data = configMap.getData();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigResource that = (ConfigResource) o;

        if (!kind.equals(that.kind)) return false;
        if (!name.equals(that.name)) return false;
        return labels != null ? labels.equals(that.labels) : that.labels == null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKind() {
        return kind;
    }

    @Override
    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (labels != null ? labels.hashCode() : 0);
        return result;
    }

    public Map<String, String> getData() {
        return data;
    }

    @Override
    public String toString() {
        return kind + ":" + name;
    }
}
