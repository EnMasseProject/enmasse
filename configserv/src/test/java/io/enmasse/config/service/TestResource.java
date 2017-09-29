package io.enmasse.config.service;

import io.enmasse.config.service.model.Resource;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import java.util.Collections;
import java.util.Map;

public class TestResource extends Resource {
    private final String name;
    private final Map<String, String> labels;
    private final String value;

    public TestResource(String name, Map<String, String> labelMap, String value) {
        this.name = name;
        this.labels = labelMap;
        this.value = value;
    }

    public TestResource(ConfigMap value) {
        this(value.getMetadata().getName(), value.getMetadata().getLabels(), value.getData().get("value"));
    }

    public TestResource(TestValue value) {
        this(value.getMetadata().getName(), value.getMetadata().getLabels(), value.value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestResource that = (TestResource) o;

        if (!name.equals(that.name)) return false;
        if (!labels.equals(that.labels)) return false;
        return value.equals(that.value);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKind() {
        return "testresource";
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + labels.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static class TestValue extends ConfigMap {
        private ObjectMeta meta = new ObjectMeta();
        private final String value;

        public TestValue(String name, Map<String, String> labels, Map<String, String> annotations, String value) {
            meta.setName(name);
            meta.setLabels(labels);
            meta.setAnnotations(annotations);
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public ObjectMeta getMetadata() {
            return meta;
        }

        @Override
        public Map<String, String> getData() {
            return Collections.singletonMap("value", value);
        }

        @Override
        public void setMetadata(ObjectMeta objectMeta) {
            this.meta = objectMeta;
        }

        @Override
        public String getKind() {
            return "mock";
        }

        @Override
        public String getApiVersion() {
            return "v1test";
        }
    }
}
