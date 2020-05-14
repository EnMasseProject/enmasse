/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.monitoring;

import com.google.common.collect.Ordering;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.apiclients.PrometheusApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.operator.EnmasseOperatorManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class MonitoringTest extends TestBase implements ITestIsolatedStandard {

    private static final int TIMEOUT_QUERY_RESULT_MINUTES = 3;
    private static final String ENMASSE_ADDRESS_SPACES_NOT_READY = "enmasse_address_space_status_not_ready";
    private static final String ENMASSE_ADDRESS_SPACES_READY = "enmasse_address_space_status_ready";
    private static Logger log = CustomLogger.getLogger();
    private PrometheusApiClient prometheusApiClient;

    @BeforeEach
    void installMonitoring() throws Exception {
        EnmasseOperatorManager.getInstance().installMonitoringOperator();
        Endpoint prometheusEndpoint = Kubernetes.getInstance().getExternalEndpoint("prometheus-route", environment.getMonitoringNamespace());
        this.prometheusApiClient = new PrometheusApiClient(prometheusEndpoint);
        waitUntilPrometheusReady();
    }

    @AfterEach
    void uninstallMonitoring() throws Exception {
        EnmasseOperatorManager.getInstance().deleteMonitoringOperator();
    }

    @Test
    @OpenShift
    void testAddressSpaceRules() throws Exception {
        Instant startTs = Instant.now();
        String testNamespace = "monitoring-test";
        kubernetes.createNamespace(testNamespace);
        String addressSpaceName = "monitoring-address-space";
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(addressSpaceName)
                .withNamespace(testNamespace)
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);

        validateAddressSpaceQueryWaiting(ENMASSE_ADDRESS_SPACES_READY, "1");

        validateAddressSpaceQueryWaiting(ENMASSE_ADDRESS_SPACES_NOT_READY, "0");

        //tests address spaces ready goes from 0 to 1
        validateAddressSpaceRangeQueryWaiting(ENMASSE_ADDRESS_SPACES_READY, startTs, range -> Ordering.natural().isOrdered(range));

        //tests address spaces not ready goes from 1 to 0
        validateAddressSpaceRangeQueryWaiting(ENMASSE_ADDRESS_SPACES_NOT_READY, startTs, range -> Ordering.natural().reverse().isOrdered(range));
    }

    private void waitUntilPrometheusReady() {
        TestUtils.waitUntilCondition("Prometheus ready", phase -> {
            try {
                JsonObject rules = prometheusApiClient.getRules();
                if (rules.getString("status", "").equals("success")) {
                    JsonObject data = rules.getJsonObject("data", new JsonObject());
                    for (Object obj : data.getJsonArray("groups", new JsonArray())) {
                        JsonObject group = (JsonObject) obj;
                        for (Object ruleObj : group.getJsonArray("rules", new JsonArray())) {
                            JsonObject rule = (JsonObject) ruleObj;
                            if (rule.getString("name").equals("enmasse_address_spaces_ready_total")) {
                                return true;
                            }
                        }
                    }
                }
                if (phase == WaitPhase.LAST_TRY) {
                    log.info("Prometheus rules obtained : {}", rules.encodePrettily());
                }
            } catch (Exception e) {
                if (phase == WaitPhase.LAST_TRY) {
                    log.error("Waiting for prometheus to be ready", e);
                }
            }
            return false;
        }, new TimeoutBudget(10, TimeUnit.MINUTES));

    }

    private void validateAddressSpaceQueryWaiting(String query, String expectedValue) throws Exception {
        TestUtils.waitUntilCondition(query, phase -> {
            try {
                validateAddressSpaceQuery(query, expectedValue);
                return true;
            } catch (Exception e) {
                if (phase == WaitPhase.LAST_TRY) {
                    log.error("Exception waiting for query " + query, e);
                }
                return false;
            }
        }, new TimeoutBudget(TIMEOUT_QUERY_RESULT_MINUTES, TimeUnit.MINUTES));
    }

    private void validateAddressSpaceRangeQueryWaiting(String query, Instant start, Predicate<List<String>> rangeValidator) throws Exception {
        TestUtils.waitUntilCondition(query, phase -> {
            try {
                validateAddressSpaceRangeQuery(query, start, query, rangeValidator);
                return true;
            } catch (Exception e) {
                if (phase == WaitPhase.LAST_TRY) {
                    log.error("Exception waiting for range query " + query, e);
                }
                return false;
            }
        }, new TimeoutBudget(TIMEOUT_QUERY_RESULT_MINUTES, TimeUnit.MINUTES));
    }

    private void validateAddressSpaceQuery(String query, String expectedValue) throws Exception {
        JsonObject queryResult = prometheusApiClient.doQuery(query);
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

    private void validateAddressSpaceRangeQuery(String query, Instant start, String addressSpace, Predicate<List<String>> rangeValidator) throws Exception {
        JsonObject queryResult = prometheusApiClient.doRangeQuery(query, String.valueOf(start.getEpochSecond()), String.valueOf(Instant.now().getEpochSecond()));
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

    private void basicQueryResultValidation(String query, JsonObject queryResult) throws Exception {
        if (queryResult == null) {
            throw new Exception("Result of query " + query + " is null");
        }
        if (!queryResult.getString("status", "").equals("success")) {
            throw new Exception("Failed doing query " + queryResult.encodePrettily());
        }
    }

    private boolean metricQueryResultValidation(JsonObject queryResult, String metricName, Predicate<JsonObject> resultValidator) {
        JsonObject data = queryResult.getJsonObject("data", new JsonObject());
        for (Object result : data.getJsonArray("result", new JsonArray())) {
            JsonObject jsonResult = (JsonObject) result;
            if (jsonResult.getJsonObject("metric", new JsonObject()).getString("__name__", "").equals(metricName)) {
                return resultValidator.test(jsonResult);
            }
        }
        return false;
    }

}
