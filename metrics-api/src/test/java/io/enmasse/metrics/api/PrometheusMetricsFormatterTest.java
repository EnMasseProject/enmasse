/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PrometheusMetricsFormatterTest {

    private static String appPrefix;

    @BeforeAll
    public static void setup() {
        System.out.println("Starting tests");

        try (java.io.InputStream inputStream = PrometheusMetricsFormatter.class.getResourceAsStream("/metrics.properties");){
            if (inputStream == null) {
                throw new IllegalStateException();
            }
            java.util.Properties properties = new Properties();
            properties.load(inputStream);
            appPrefix = properties.getProperty("appPrefix");
        } catch (IOException e) {
            System.out.println("Error reading app prefix {}");
            throw new IllegalStateException();
        }
    }

    @Test
    public void testEscaping() {
        PrometheusMetricsFormatter formatter = new PrometheusMetricsFormatter();

        Metric metric = new Metric("m1", "mdesc", MetricType.gauge, new MetricValue(2, 3, new MetricLabel("key1", "value1"), new MetricLabel("key2", "\\this\"is\nescaped\\")));

        String value = formatter.format(Collections.singletonList(metric));
        System.out.println(value);
        String expected = "# HELP " + appPrefix + "_m1 mdesc\n# TYPE " + appPrefix + "_m1 gauge\n" + appPrefix + "_m1{key1=\"value1\",key2=\"\\\\this\\\"is\\nescaped\\\\\"} 2 3\n";
        assertEquals(expected, value);
    }

    @Test
    public void testEmpty() {
        Metrics metrics = new Metrics();
        metrics.reportMetric(new Metric(
                "address_spaces_ready_total",
                "Total number of address spaces in ready state",
                MetricType.gauge,
                Collections.emptyList()));
        metrics.reportMetric(new Metric(
                "address_spaces_not_ready_total",
                "Total number of address spaces in a not ready state",
                MetricType.gauge,
                Collections.emptyList()));
        metrics.reportMetric(new Metric(
                "address_spaces_total",
                "Total number of address spaces",
                MetricType.gauge,
                new MetricValue(0, 0)));

        PrometheusMetricsFormatter formatter = new PrometheusMetricsFormatter();
        System.out.print(formatter.format(metrics.snapshot()));
    }
}
