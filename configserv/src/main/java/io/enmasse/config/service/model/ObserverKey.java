/*
 * Copyright 2017 Red Hat Inc.
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
