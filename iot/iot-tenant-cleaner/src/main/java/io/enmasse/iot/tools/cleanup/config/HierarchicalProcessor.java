/*
 *  Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup.config;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;

class HierarchicalProcessor {

    private Pattern splitter;
    private Function<String, String> keyProcesor;

    public static Function<JsonObject, JsonObject> defaultProcessor() {
        var processor = new HierarchicalProcessor(Pattern.compile("[\\._]+"), key -> key);
        return json -> processor.process(json);
    }

    public HierarchicalProcessor(final Pattern splitter, final Function<String, String> keyProcessor) {

        Objects.requireNonNull(splitter);
        Objects.requireNonNull(keyProcessor);

        this.splitter = splitter;
        this.keyProcesor = keyProcessor;

    }

    public JsonObject process(final JsonObject json) {

        if (json == null || json.isEmpty()) {
            return json;
        }

        final JsonObject result = new JsonObject();

        json.forEach(e -> {
            put(result, split(e.getKey()), e.getValue());
        });

        return result;

    }

    private void put(final JsonObject object, final Queue<String> path, final Object value) {
        String key = this.keyProcesor.apply(path.poll());
        if (path.isEmpty()) {
            object.put(key, value);
        } else {
            Object currentValue = object.getValue(key);
            final JsonObject child;
            if (!(currentValue instanceof JsonObject)) {
                child = new JsonObject();
                object.put(key, child);
            } else {
                child = (JsonObject) currentValue;
            }
            put(child, path, value);
        }
    }

    private Queue<String> split(final String key) {
        return new LinkedList<>(this.splitter.splitAsStream(key)
                .collect(Collectors.toList()));
    }
}
