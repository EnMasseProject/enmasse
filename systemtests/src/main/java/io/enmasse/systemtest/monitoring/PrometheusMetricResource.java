/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.monitoring;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class PrometheusMetricResource {
    String name;
    Map<String, String> metadata;
    String value;
    List<String> rangeValues;

    private PrometheusMetricResource(String name, Map<String, String> metadata, String value) {
        this.name = name;
        this.metadata = metadata;
        this.value = value;
    }

    private PrometheusMetricResource(String name, Map<String, String> metadata, List<String> rangeValues) {
        this.name = name;
        this.metadata = metadata;
        this.rangeValues = rangeValues;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getMetadataValue(String metadataName) {
        return metadata.get(metadataName);
    }

    public String getValue() {
        return value;
    }

    public List<String> getRangeValues() {
        return rangeValues;
    }

    private static Map<String, String> parseMetadata(JsonObject data) {
        HashMap<String, String> metadata = new HashMap<>();
        data.forEach(set -> {
            metadata.put(set.getKey(), set.getValue().toString());
        });
        return metadata;
    }

    public static PrometheusMetricResource getResource(JsonObject queryResult, String metricName, Map<String, String> selector) {
        JsonObject data = queryResult.getJsonObject("data", new JsonObject());
        for (Object result : data.getJsonArray("result", new JsonArray())) {
            JsonObject jsonResult = (JsonObject) result;
            if (jsonResult.getJsonObject("metric", new JsonObject()).getString("__name__", "").equals(metricName)) {
                boolean match = true;
                for (Map.Entry<String, String> label : selector.entrySet()) {
                    if (!jsonResult.getJsonObject("metric", new JsonObject()).getString(label.getKey(), "").equals(label.getValue())) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    if (jsonResult.containsKey("values")) {
                        return new PrometheusMetricResource(
                                metricName,
                                parseMetadata(jsonResult.getJsonObject("metric", new JsonObject())),
                                jsonResult.getJsonArray("values", new JsonArray()).stream().map(obj -> (JsonArray) obj).map(array -> array.getString(1)).collect(Collectors.toList())
                        );
                    } else {
                        return new PrometheusMetricResource(
                                metricName,
                                parseMetadata(jsonResult.getJsonObject("metric", new JsonObject())),
                                jsonResult.getJsonArray("value", new JsonArray()).getString(1)
                        );
                    }
                }
            }
        }
        return null;
    }
}
