/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.logs.CustomLogger;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ScaleTestEnvironment {

    private static Logger log = CustomLogger.getLogger();
    private static JsonNode jsonEnv;

    private static final String SCALE_SLEEP_PER_CONNECTION_MILLIS = "SCALE_SLEEP_PER_CONNECTION_MILLIS";
    private static final String SCALE_ADDRESSES_PER_TENANT = "SCALE_ADDRESSES_PER_TENANT";

    private static final String SCALE_SEND_MSG_PERIOD_MILLIS = "SCALE_SEND_MSG_PERIOD_MILLIS";
    private static final String SCALE_ADDRESSES_FAILURE_THRESHOLD = "SCALE_ADDRESSES_FAILURE_THRESHOLD";

    private static final String SCALE_PERF_INIT_ADDRESSES = "SCALE_PERF_INIT_ADDRESSES";
    private static final String SCALE_PERF_INIT_ADDRESSES_PER_GROUP = "SCALE_PERF_INIT_ADDRESSES_PER_GROUP";
    private static final String SCALE_PERF_ADDRESSES_PER_GROUP_INC = "SCALE_PERF_ADDRESSES_PER_GROUP_INC";
    private static final String SCALE_PERF_INIT_ANYCAST_LINKS_PER_CONN = "SCALE_PERF_INIT_ANYCAST_LINKS_PER_CONN";
    private static final String SCALE_PERF_ANYCAST_LINKS_PER_CONN_INC = "SCALE_PERF_ANYCAST_LINKS_PER_CONN_INC";
    private static final String SCALE_PERF_INIT_QUEUE_LINKS_PER_CONN = "SCALE_PERF_INIT_QUEUE_LINKS_PER_CONN";
    private static final String SCALE_PERF_QUEUE_LINKS_PER_CONN_INC = "SCALE_PERF_QUEUE_LINKS_PER_CONN_INC";
    private static final String SCALE_PERF_SEND_MSG_PERIOD_MILLIS = "SCALE_PERF_SEND_MSG_PERIOD_MILLIS";
    private static final String SCALE_PERF_RECEIVERS_PER_TENANT = "SCALE_PERF_RECEIVERS_PER_TENANT";
    private static final String SCALE_PERF_SENDERS_PER_TENANT = "SCALE_PERF_SENDERS_PER_TENANT";

    private static final String SCALE_HA_ADDRESSES = "SCALE_HA_ADDRESSES";
    private static final String SCALE_HA_ADDRESSES_PER_GROUP = "SCALE_HA_ADDRESSES_PER_GROUP";
    private static final String SCALE_HA_SEND_MSG_PERIOD_MILLIS = "SCALE_HA_SEND_MSG_PERIOD_MILLIS";

    private static final String SCALE_METRICS_CONN_FAIL_RATIO_THRESHOLD = "SCALE_METRICS_CONN_FAIL_RATIO_THRESHOLD";
    private static final String SCALE_METRICS_RECONN_FAIL_RATIO_THRESHOLD = "SCALE_METRICS_RECONN_FAIL_RATIO_THRESHOLD";
    private static final String SCALE_METRICS_NOT_ACCEPTED_RATIO_THRESHOLD = "SCALE_METRICS_NOT_ACCEPTED_RATIO_THRESHOLD";
    private static final String SCALE_METRICS_SCRAPE_RETRIES = "SCALE_METRICS_SCRAPE_RETRIES";
    private static final String SCALE_METRICS_SCRAPE_PERIOD_MILLIS = "SCALE_METRICS_SCRAPE_PERIOD_MILLIS";

    private List<Map.Entry<String, String>> values = new ArrayList<>();

    //general purpose
    private final int addressesPerTenant = getOrDefault(SCALE_ADDRESSES_PER_TENANT, Integer::parseInt, 5);
    private final int sleepPerConnectionMillis = getOrDefault(SCALE_SLEEP_PER_CONNECTION_MILLIS, Integer::parseInt, 4);

    //addresses and connections scaling constants
    private final int scaleSendMessagesPeriod = getOrDefault(SCALE_SEND_MSG_PERIOD_MILLIS, Integer::parseInt, 10_000);
    private final int scaleAddressesFailureThreshold = getOrDefault(SCALE_ADDRESSES_FAILURE_THRESHOLD, Integer::parseInt, 12_000);

    //performance test constants
    private final int performanceInitialAddresses = getOrDefault(SCALE_PERF_INIT_ADDRESSES, Integer::parseInt, 12_000);
    private final int performanceInitialAddressesPerGroup = getOrDefault(SCALE_PERF_INIT_ADDRESSES_PER_GROUP, Integer::parseInt, 100);
    private final int performanceAddressesPerGroupIncrease = getOrDefault(SCALE_PERF_ADDRESSES_PER_GROUP_INC, Integer::parseInt, 100);
    private final int performanceInitialAnycastLinksPerConn = getOrDefault(SCALE_PERF_INIT_ANYCAST_LINKS_PER_CONN, Integer::parseInt, 1);
    private final int performanceAnycastLinksPerConnIncrease = getOrDefault(SCALE_PERF_ANYCAST_LINKS_PER_CONN_INC, Integer::parseInt, 1);
    private final int performanceInitialQueueLinksPerConn = getOrDefault(SCALE_PERF_INIT_QUEUE_LINKS_PER_CONN, Integer::parseInt, 1);
    private final int performanceQueueLinksPerConnIncrease = getOrDefault(SCALE_PERF_QUEUE_LINKS_PER_CONN_INC, Integer::parseInt, 1);
    /**
     * For better syncronization and accuracy of metrics values it's recommended
     * that this value is less than  the metrics scrapping period
     */
    private final int performanceSendMessagesPeriod = getOrDefault(SCALE_PERF_SEND_MSG_PERIOD_MILLIS, Integer::parseInt, 10_000);
    private final int performanceReceiversPerTenant = getOrDefault(SCALE_PERF_RECEIVERS_PER_TENANT, Integer::parseInt, 1);
    private final int performanceSendersPerTenant = getOrDefault(SCALE_PERF_SENDERS_PER_TENANT, Integer::parseInt, 1);

    //fault tolerance constants
    private final int faultToleranceInitialAddresses = getOrDefault(SCALE_HA_ADDRESSES, Integer::parseInt, 8_000);
    private final int faultToleranceAddressesPerGroup = getOrDefault(SCALE_HA_ADDRESSES_PER_GROUP, Integer::parseInt, 100);
    private final int faultToleranceSendMessagesPeriod = getOrDefault(SCALE_HA_SEND_MSG_PERIOD_MILLIS, Integer::parseInt, 20_000);

    //metrics monitoring constants
    private final double connectionFailureRatioThreshold = getOrDefault(SCALE_METRICS_CONN_FAIL_RATIO_THRESHOLD, Double::parseDouble, 0.45);
    private final double reconnectFailureRatioThreshold = getOrDefault(SCALE_METRICS_RECONN_FAIL_RATIO_THRESHOLD, Double::parseDouble, 0.45);
    private final double notAcceptedDeliveriesRatioThreshold = getOrDefault(SCALE_METRICS_NOT_ACCEPTED_RATIO_THRESHOLD, Double::parseDouble, 0.5);

    //metrics scraping
    private final int scrapeRetries = getOrDefault(SCALE_METRICS_SCRAPE_RETRIES, Integer::parseInt, 2);
    private final long metricsUpdatePeriodMillis = getOrDefault(SCALE_METRICS_SCRAPE_PERIOD_MILLIS, Long::parseLong, 11000l);

    //singelton
    private static ScaleTestEnvironment instance;

    public static synchronized ScaleTestEnvironment getInstance() {
        if (instance == null) {
            loadJsonEnv();
            instance = new ScaleTestEnvironment();
        }
        return instance;
    }

    private ScaleTestEnvironment() {
        String logFormat = "{}:{}";
        values.forEach(v -> log.info(logFormat, v.getKey(), v.getValue()));
    }

    public int getAddressesPerTenant() {
        return addressesPerTenant;
    }

    public int getSleepPerConnectionMillis() {
        return sleepPerConnectionMillis;
    }

    public int getScaleSendMessagesPeriod() {
        return scaleSendMessagesPeriod;
    }

    public int getScaleAddressesFailureThreshold() {
        return scaleAddressesFailureThreshold;
    }

    public int getPerfInitialAddresses() {
        return performanceInitialAddresses;
    }

    public int getPerfInitialAddressesPerGroup() {
        return performanceInitialAddressesPerGroup;
    }

    public int getPerfAddressesPerGroupIncrease() {
        return performanceAddressesPerGroupIncrease;
    }

    public int getPerfInitialAnycastLinksPerConn() {
        return performanceInitialAnycastLinksPerConn;
    }

    public int getPerfAnycastLinksPerConnIncrease() {
        return performanceAnycastLinksPerConnIncrease;
    }

    public int getPerfInitialQueueLinksPerConn() {
        return performanceInitialQueueLinksPerConn;
    }

    public int getPerfQueueLinksPerConnIncrease() {
        return performanceQueueLinksPerConnIncrease;
    }

    public int getPerfSendMessagesPeriod() {
        return performanceSendMessagesPeriod;
    }

    public int getPerfReceiversPerTenant() {
        return performanceReceiversPerTenant;
    }

    public int getPerfSendersPerTenant() {
        return performanceSendersPerTenant;
    }


    public int getFaultToleranceInitialAddresses() {
        return faultToleranceInitialAddresses;
    }

    public int getFaultToleranceAddressesPerGroup() {
        return faultToleranceAddressesPerGroup;
    }

    public int getFaultToleranceSendMessagesPeriod() {
        return faultToleranceSendMessagesPeriod;
    }

    public double getConnectionFailureRatioThreshold() {
        return connectionFailureRatioThreshold;
    }

    public double getReconnectFailureRatioThreshold() {
        return reconnectFailureRatioThreshold;
    }

    public double getNotAcceptedDeliveriesRatioThreshold() {
        return notAcceptedDeliveriesRatioThreshold;
    }

    public int getScrapeRetries() {
        return scrapeRetries;
    }

    public long getMetricsUpdatePeriodMillis() {
        return metricsUpdatePeriodMillis;
    }

    private <T> T getOrDefault(String var, Function<String, T> converter, T defaultValue) {
        String value = System.getenv(var) != null ? System.getenv(var) : (jsonEnv.get(var) != null ? jsonEnv.get(var).asText() : null);
        T returnValue = defaultValue;
        if (value != null) {
            returnValue = converter.apply(value);
        }
        values.add(Map.entry(var, String.valueOf(returnValue)));
        return returnValue;
    }

    private static void loadJsonEnv() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File jsonFile = new File(Environment.getInstance().getScaleConfig()).getAbsoluteFile();
            jsonEnv = mapper.readTree(jsonFile);
        } catch (Exception e) {
            log.warn("Scale json configuration not provider or not exists");
            jsonEnv = mapper.createObjectNode();
        }
    }

}