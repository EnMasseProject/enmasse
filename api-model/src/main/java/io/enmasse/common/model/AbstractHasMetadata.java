/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.common.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"))
@SuppressWarnings("serial")
public abstract class AbstractHasMetadata<T> extends AbstractResource<T> implements HasMetadata {

    @FunctionalInterface
    private static interface Putter {
        public <K, V> void put(Map<K, V> map, K key, V value);
    }

    @NotNull @Valid
    private ObjectMeta metadata = new ObjectMeta();

    protected AbstractHasMetadata(final String kind, String apiVersion) {
        super(kind, apiVersion);
    }

    @Override
    public ObjectMeta getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(final ObjectMeta metadata) {
        this.metadata = metadata;
    }

    private String getMeta(final String key, final Function<ObjectMeta,Map<String,String>> getter) {
        if (this.metadata == null) {
            return null;
        }

        final Map<String,String> map = getter.apply(this.metadata);
        if (map == null) {
            return null;
        }

        return map.get(key);
    }

    private void removeMeta(final String key, final Function<ObjectMeta,Map<String,String>> getter) {
        if (this.metadata == null) {
            return;
        }

        final Map<String,String> map = getter.apply(this.metadata);
        if (map == null) {
            return;
        }

        map.remove(key);
    }

    private void putMeta(final String key, final String value, final Function<ObjectMeta,Map<String,String>> getter, final BiConsumer<ObjectMeta, Map<String,String>> setter, final Putter putter) {
        if (this.metadata == null) {
            this.metadata = new ObjectMeta();
        }

        Map<String,String> map = getter.apply(this.metadata);
        if (map == null) {
            map = new HashMap<>();
            setter.accept(this.metadata, map);
        }

        putter.put(map, key, value);
    }

    public String getAnnotation(final String key) {
       return getMeta(key, ObjectMeta::getAnnotations);
    }

    public void removeAnnotation(final String key) {
      removeMeta(key, ObjectMeta::getAnnotations);
    }

    public void putAnnotation(final String key, final String value) {
        putMeta(key, value, ObjectMeta::getAnnotations, ObjectMeta::setAnnotations, Map::put);
    }

    public void putAnnotationIfAbsent(final String key, final String value) {
        putMeta(key, value, ObjectMeta::getAnnotations, ObjectMeta::setAnnotations, Map::putIfAbsent);
    }

    public String getLabel(final String key) {
        return getMeta(key, ObjectMeta::getLabels);
     }

     public void removeLabel(final String key) {
       removeMeta(key, ObjectMeta::getLabels);
     }

     public void putLabel(final String key, final String value) {
         putMeta(key, value, ObjectMeta::getLabels, ObjectMeta::setLabels, Map::put);
     }

     public void putLabelIfAbsent(final String key, final String value) {
         putMeta(key, value, ObjectMeta::getLabels, ObjectMeta::setLabels, Map::putIfAbsent);
     }

}
