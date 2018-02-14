/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.model;

import java.util.Map;

/**
 * A key uniquely identifying an observer
 */
public class ObserverKey {
    private final Map<String, String> labelFilter;
    private final Map<String, String> annotationFilter;

    public ObserverKey(Map<String, String> labelFilter, Map<String, String> annotationFilter) {
        this.labelFilter = labelFilter;
        this.annotationFilter = annotationFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObserverKey that = (ObserverKey) o;

        if (!labelFilter.equals(that.labelFilter)) return false;
        return annotationFilter.equals(that.annotationFilter);
    }

    @Override
    public int hashCode() {
        int result = labelFilter.hashCode();
        result = 31 * result + annotationFilter.hashCode();
        return result;
    }

    public Map<String, String> getLabelFilter() {
        return labelFilter;
    }

    public Map<String, String> getAnnotationFilter() {
        return annotationFilter;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("labels=").append(labelFilter)
                .append("annotations=").append(annotationFilter);
        return str.toString();
    }
}
