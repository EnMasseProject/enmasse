package enmasse.config.service;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import java.util.Map;

public class TestResource implements HasMetadata {
    private final ObjectMeta meta = new ObjectMeta();

    public TestResource(String name, Map<String, String> labelMap) {
        meta.setName(name);
        meta.setLabels(labelMap);
    }

    @Override
    public ObjectMeta getMetadata() {
        return meta;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {

    }

    @Override
    public String getKind() {
        return "test";
    }

    @Override
    public String getApiVersion() {
        return "v1enmasse";
    }
}
