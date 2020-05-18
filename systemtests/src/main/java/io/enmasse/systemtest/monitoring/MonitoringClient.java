/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.monitoring;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.apiclients.PrometheusApiClient;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MonitoringClient {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private final PrometheusApiClient client;

    public MonitoringClient(Endpoint endpoint) {
        this.client = new PrometheusApiClient(endpoint);
    }

    public void validateRangeQueryAndWait(String query, String expectedValue) {
        TestUtils.waitUntilCondition(query, phase -> {
            try {
                validateQuery(query, expectedValue);
                return true;
            } catch (Exception e) {
                if (phase == WaitPhase.LAST_TRY) {
                    LOGGER.error("Exception waiting for query " + query, e);
                }
                return false;
            }
        }, new TimeoutBudget(3, TimeUnit.MINUTES));
    }

    public void validateRangeQueryAndWait(String query, Instant start, Predicate<List<String>> rangeValidator) {
        TestUtils.waitUntilCondition(query, phase -> {
            try {
                validateRangeQuery(query, start, query, rangeValidator);
                return true;
            } catch (Exception e) {
                if (phase == WaitPhase.LAST_TRY) {
                    LOGGER.error("Exception waiting for range query " + query, e);
                }
                return false;
            }
        }, new TimeoutBudget(3, TimeUnit.MINUTES));
    }

    public void validateQuery(String query, String expectedValue) throws Exception {
        JsonObject queryResult = client.doQuery(query);
        basicQueryResultValidation(query, queryResult);
        boolean validateResult = metricQueryResultValidation(queryResult, query, jsonResult -> {
            JsonArray valueArray = jsonResult.getJsonArray("value", new JsonArray());
            return valueArray.size() == 2 && valueArray.getString(1).equals(expectedValue);
        });
        if (validateResult) {
            return;
        }
        throw new Exception("Unexpected query result " + queryResult.encodePrettily());
    }

    public void validateRangeQuery(String query, Instant start, String addressSpace, Predicate<List<String>> rangeValidator) throws Exception {
        JsonObject queryResult = client.doRangeQuery(query, String.valueOf(start.getEpochSecond()), String.valueOf(Instant.now().getEpochSecond()));
        basicQueryResultValidation(query, queryResult);
        boolean validateResult = metricQueryResultValidation(queryResult, addressSpace, jsonResult -> {
            JsonArray valuesArray = jsonResult.getJsonArray("values", new JsonArray());
            return rangeValidator.test(valuesArray.stream().map(obj -> (JsonArray) obj).map(array -> array.getString(1)).collect(Collectors.toList()));
        });
        if (validateResult) {
            return;
        }
        throw new Exception("Unexpected query result " + queryResult.encodePrettily());
    }

    public void waitUntilPrometheusReady() {
        TestUtils.waitUntilCondition("Prometheus ready", phase -> {
            try {
                JsonObject rule = getRule("enmasse_address_spaces_ready_total");
                if (rule != null) {
                    return true;
                }
                if (phase == WaitPhase.LAST_TRY) {
                    LOGGER.info("Prometheus rules obtained : {}", client.getRules().encodePrettily());
                }
            } catch (Exception e) {
                if (phase == WaitPhase.LAST_TRY) {
                    LOGGER.error("Waiting for prometheus to be ready", e);
                }
            }
            return false;
        }, new TimeoutBudget(10, TimeUnit.MINUTES));
    }

    //=============================================================================
    // Help methods
    //=============================================================================
    private void basicQueryResultValidation(String query, JsonObject queryResult) throws Exception {
        if (queryResult == null) {
            throw new Exception("Result of query " + query + " is null");
        }
        if (!queryResult.getString("status", "").equals("success")) {
            throw new Exception("Failed doing query " + queryResult.encodePrettily());
        }
    }

    private boolean metricQueryResultValidation(JsonObject queryResult, String metricName, Predicate<JsonObject> resultValidator) {
        JsonObject data = getResults(queryResult, metricName);
        if (data != null) {
            return resultValidator.test(data);
        }
        return false;
    }

    private JsonObject getResults(JsonObject queryResult, String metricName) {
        JsonObject data = queryResult.getJsonObject("data", new JsonObject());
        for (Object result : data.getJsonArray("result", new JsonArray())) {
            JsonObject jsonResult = (JsonObject) result;
            if (jsonResult.getJsonObject("metric", new JsonObject()).getString("__name__", "").equals(metricName)) {
                return jsonResult;
            }
        }
        return null;
    }

    private JsonObject getRule(String name) throws Exception {
        JsonObject rules = client.getRules();
        if (rules.getString("status", "").equals("success")) {
            JsonObject data = rules.getJsonObject("data", new JsonObject());
            for (Object obj : data.getJsonArray("groups", new JsonArray())) {
                JsonObject group = (JsonObject) obj;
                for (Object ruleObj : group.getJsonArray("rules", new JsonArray())) {
                    JsonObject rule = (JsonObject) ruleObj;
                    if (rule.getString("name").equals(name)) {
                        return rule;
                    }
                }
            }
        }
        return null;
    }
}
