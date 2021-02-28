/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.common.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;


import io.enmasse.admin.model.v1.WithAdditionalProperties;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.sundr.builder.annotations.Buildable;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@SuppressWarnings("serial")
public abstract class CustomResourceWithAdditionalProperties<S, T> extends CustomResource<S, T> implements WithAdditionalProperties {

    @FunctionalInterface
    private static interface Putter {
        public <K, V> void put(Map<K, V> map, K key, V value);
    }

    private Map<String, Object> additionalProperties;

    private String getMeta(final String key, final Function<ObjectMeta,Map<String,String>> getter) {
        if (this.getMetadata() == null) {
            return null;
        }

        final Map<String,String> map = getter.apply(this.getMetadata());
        if (map == null) {
            return null;
        }

        return map.get(key);
    }

    public void putAnnotation(final String key, final String value) {
        putMeta(key, value, ObjectMeta::getAnnotations, ObjectMeta::setAnnotations, Map::put);
    }

    private void putMeta(final String key, final String value, final Function<ObjectMeta,Map<String,String>> getter, final BiConsumer<ObjectMeta, Map<String,String>> setter, final Putter putter) {
        if (this.getMetadata() == null) {
            this.setMetadata(new ObjectMeta());
        }

        Map<String,String> map = getter.apply(this.getMetadata());
        if (map == null) {
            map = new HashMap<>();
            setter.accept(this.getMetadata(), map);
        }

        putter.put(map, key, value);
    }

    public String getAnnotation(final String key) {
       return getMeta(key, ObjectMeta::getAnnotations);
    }

    @Override
    public final Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @Override
    public final void setAdditionalProperties(Map<String, Object> additionalProperties) {
        if (additionalProperties == null) {
            this.additionalProperties = null;
        } else {
            this.additionalProperties = new HashMap<>(additionalProperties);
        }
    }

    @Override
    public final void setAdditionalProperty(String name, Object value) {
        if (this.additionalProperties == null) {
            this.additionalProperties = new HashMap<>();
        }
        this.additionalProperties.put(name, value);
    }
}
