/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.framework;

import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogChecker {
    private static final Logger LOGGER = LoggerUtils.getLogger();
    private static Pattern operatorPattern = Pattern.compile("\"ts\":(\\d+\\.?\\d*),"); //example: 1595410833.766529
    private static Pattern routerPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+)"); //example: 2020-07-22 09:40:13.568035
    private static Pattern brokerPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z)"); //example: 2020-07-22T09:45:00.166Z
    private static Matcher matcher;

    public static Map<String, List> getErrorsFromPodsLogsInNamespace(String namespace, Timestamp startTime, Timestamp stopTime) {
        ZonedDateTime startInstant = startTime.toInstant().atZone(ZoneId.of("UTC")).minusYears(1900).minusMonths(1).minusHours(4);
        ZonedDateTime stopInstant = stopTime.toInstant().atZone(ZoneId.of("UTC")).minusYears(1900).minusMonths(1).minusHours(4);
        LOGGER.info(String.format("Checking pods' logs in namespace %s for errors, from time %s to time %s", namespace, startInstant.toString(), stopInstant.toString()));
        List<Pod> pods = Kubernetes.getInstance().listPods(namespace);
        Map<String, List> podErrors = new HashMap<>();
        for (Pod pod : pods) {
            String podLog = Kubernetes.getClient().pods().inNamespace(namespace).withName(pod.getMetadata().getName()).getLog();
            String[] logLines = podLog.lines().toArray(String[]::new);
            Instant instant;
            List<String> errorLines = new ArrayList<>();
            for (String logLine : logLines) {
                instant = matchPattern(logLine);
                if (instant == null) continue;
                ZonedDateTime zonedInstant= instant.atZone(ZoneId.of("UTC"));
                if (zonedInstant.isAfter(startInstant) && zonedInstant.isBefore(stopInstant)) {
                    if (logLine.contains("\"level\":\"error\"") | logLine.contains("(error)") | logLine.contains("ERROR")) {
                        errorLines.add(logLine);
                    }
                }
            }
            if (!errorLines.isEmpty()) {
                podErrors.put(pod.getMetadata().getName(), errorLines);
            }
        }
        return podErrors;
    }

    public static Map<String, List> getErrorsFromPodLog(String namespace, String podName, Timestamp startTime, Timestamp stopTime) {
        ZonedDateTime startInstant = startTime.toInstant().atZone(ZoneId.of("UTC")).minusYears(1900).minusMonths(1).minusHours(4);
        ZonedDateTime stopInstant = stopTime.toInstant().atZone(ZoneId.of("UTC")).minusYears(1900).minusMonths(1).minusHours(4);
        LOGGER.info(String.format("Checking pod's %s logs in namespace %s for errors, from time %s to time %s", podName, namespace, startInstant.toString(), stopInstant.toString()));
        Map<String, List> podErrors = new HashMap<>();
        String podLog = Kubernetes.getClient().pods().inNamespace(namespace).withName(podName).getLog();
        String[] logLines = podLog.lines().toArray(String[]::new);
        Instant instant;
        List<String> errorLines = new ArrayList<>();
        for (String logLine : logLines) {
            instant = matchPattern(logLine);
            if (instant == null) continue;
            ZonedDateTime zonedInstant= instant.atZone(ZoneId.of("UTC"));
            if (zonedInstant.isAfter(startInstant) && zonedInstant.isBefore(stopInstant)) {
                if (logLine.contains("\"level\":\"error\"") | logLine.contains("(error)") | logLine.contains("ERROR")) {
                    errorLines.add(logLine);
                }
            }
        }
        if (!errorLines.isEmpty()) {
            podErrors.put(podName, errorLines);
        }
        return podErrors;
    }

    private static Instant matchPattern(String logLine) {
        Instant instant = null;
        matcher = operatorPattern.matcher(logLine);
        if (matcher.find()) {
            Double timeSec = (Double.parseDouble(matcher.group(1)));
            //convert to nanosec
            timeSec *= 1000000000;
            long timeNanoSec = timeSec.longValue();
            instant = Instant.ofEpochSecond(0, timeNanoSec);
            instant = instant.plusSeconds(7200);
        }
        matcher = routerPattern.matcher(logLine);
        if (matcher.find()) {
            Timestamp ts = Timestamp.valueOf(matcher.group(1));
            instant = ts.toInstant();
            instant = instant.minusSeconds(7200);
        }
        matcher = brokerPattern.matcher(logLine);
        if (matcher.find()) {
            instant = Instant.parse(matcher.group(1));
            instant = instant.plusSeconds(7200);
        }
        return instant;
    }
}
