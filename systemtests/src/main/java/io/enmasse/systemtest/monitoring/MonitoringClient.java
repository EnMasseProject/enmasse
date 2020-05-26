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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;


public class MonitoringClient {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private final PrometheusApiClient client;

    public MonitoringClient(Endpoint endpoint) {
        this.client = new PrometheusApiClient(endpoint);
    }

    public void validateQueryAndWait(String query, String expectedValue) {
        validateQueryAndWait(query, expectedValue, Collections.emptyMap());
    }

    public void validateQueryAndWait(String query, String expectedValue, Map<String, String> labels) {
        TestUtils.waitUntilCondition(String.format("Query: %s, expected value: %s", query, expectedValue), phase -> {
            try {
                validateQuery(query, expectedValue, labels);
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
        TestUtils.waitUntilCondition(String.format("Range query: %s, from %s to now", query, start), phase -> {
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

    public void validateQuery(String query, String expectedValue, Map<String, String> labels) throws Exception {
        JsonObject queryResult = client.doQuery(query);
        basicQueryResultValidation(query, queryResult);
        boolean validateResult = metricQueryResultValidation(queryResult, query, labels, resource -> expectedValue.equals(resource.getValue()));
        if (!validateResult) {
            throw new Exception("Unexpected query result " + queryResult.encodePrettily());
        }
    }

    public void validateRangeQuery(String query, Instant start, String addressSpace, Predicate<List<String>> rangeValidator) throws Exception {
        JsonObject queryResult = client.doRangeQuery(query, String.valueOf(start.getEpochSecond()), String.valueOf(Instant.now().getEpochSecond()));
        basicQueryResultValidation(query, queryResult);
        boolean validateResult = metricQueryResultValidation(queryResult, addressSpace, Collections.emptyMap(), resource -> rangeValidator.test(resource.getRangeValues()));
        if (!validateResult) {
            throw new Exception("Unexpected query result " + queryResult.encodePrettily());
        }
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

    public PrometheusMetricResource getQueryResult(String query, Map<String, String> labels) throws Exception {
        JsonObject response = client.doQuery(query);
        basicQueryResultValidation(query, response);
        return PrometheusMetricResource.getResource(response, query, labels);
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

    private boolean metricQueryResultValidation(JsonObject queryResult, String metricName, Map<String, String> labels, Predicate<PrometheusMetricResource> resultValidator) {
        PrometheusMetricResource data = PrometheusMetricResource.getResource(queryResult, metricName, labels);
        if (data != null) {
            return resultValidator.test(data);
        }
        return false;
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
