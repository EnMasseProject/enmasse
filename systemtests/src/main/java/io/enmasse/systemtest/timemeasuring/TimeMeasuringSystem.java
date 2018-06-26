/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.timemeasuring;

import io.enmasse.systemtest.CustomLogger;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class TimeMeasuringSystem {
    private static final Logger log = CustomLogger.getLogger();
    private static TimeMeasuringSystem instance;
    private Map<String, MeasureRecord> measuringMap;
    private String testName;

    //===============================================================
    // private instance methods
    //===============================================================
    private TimeMeasuringSystem() {
        measuringMap = new LinkedHashMap<>();
    }

    private static synchronized TimeMeasuringSystem getInstance() {
        if (instance == null) {
            instance = new TimeMeasuringSystem();
        }
        return instance;
    }

    private String createOperationsID(Operation operation) {
        String id = String.format("%s-%s", testName, operation);
        if (!operation.equals(Operation.TEST_EXECUTION)) {
            id = String.format("%s-%s", id, UUID.randomUUID().toString().split("-")[0]);
        }
        return id;
    }

    private String setStartTime(Operation operation) {
        String id = createOperationsID(operation);
        try {
            measuringMap.put(id, new MeasureRecord(System.currentTimeMillis()));
            log.info("Start time of operation {} is correctly stored", id);
        } catch (Exception ex) {
            log.warn("Start time of operation {} is not set due to exception", id);
        }
        return id;
    }

    private void setEndTime(String id) {
        if (id.equals(Operation.TEST_EXECUTION.toString())) {
            id = createOperationsID(Operation.TEST_EXECUTION);
        }
        try {
            measuringMap.get(id).setEndTime(System.currentTimeMillis());
            log.info("End time of operation {} is correctly stored", id);
        } catch (Exception ex) {
            log.warn("End time of operation {} is not set due to exception", id);
        }
    }

    private void serTestNameID(String testName) {
        this.testName = testName;
    }

    private void printAndSaveResults() {
        String tmpID = "";
        for (Map.Entry<String, MeasureRecord> record : measuringMap.entrySet()) {
            if (!record.getKey().contains(tmpID) || tmpID.isEmpty()) {
                log.info("================================================");
            }
            log.info("Operation id: {} duration: {} started: {} ended: {}",
                    record.getKey(),
                    record.getValue().getDurationHumanReadable(),
                    record.getValue().getStartTimeHumanReadable(),
                    record.getValue().getEndTimeHumanReadable());
            tmpID = record.getKey().split("-")[0];
        }
    }

    //===============================================================
    // public static methods
    //===============================================================
    public static void setTestName(String name) {
        TimeMeasuringSystem.getInstance().serTestNameID(name);
    }

    public static String startOperation(Operation operation) {
        return TimeMeasuringSystem.getInstance().setStartTime(operation);
    }

    public static void stopOperation(String operationId) {
        TimeMeasuringSystem.getInstance().setEndTime(operationId);
    }

    public static void stopOperation(Operation operationId) {
        TimeMeasuringSystem.stopOperation(operationId.toString());
    }

    public static void printResults() {
        TimeMeasuringSystem.getInstance().printAndSaveResults();
    }

    /**
     * Test time duration class
     */
    private class MeasureRecord {
        long startTime;
        long endTime;

        MeasureRecord(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        MeasureRecord(long startTime) {
            this.startTime = startTime;
        }

        long getStartTime() {
            return startTime;
        }

        long getEndTime() {
            return endTime;
        }

        void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        long getDuration() {
            return endTime - startTime;
        }

        String getDurationHumanReadable() {
            long milis = getDuration();
            long hours = TimeUnit.MILLISECONDS.toHours(milis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(milis) - (TimeUnit.MILLISECONDS.toHours(milis) * 60);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(milis) - (TimeUnit.MILLISECONDS.toMinutes(milis) * 60);
            long milliseconds = TimeUnit.MILLISECONDS.toMillis(milis) - (TimeUnit.MILLISECONDS.toSeconds(milis) * 1000);

            return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds);
        }

        String getStartTimeHumanReadable() {
            return transformMillisToDateTime(startTime);
        }

        String getEndTimeHumanReadable() {
            return transformMillisToDateTime(endTime);
        }

        private String transformMillisToDateTime(long millis) {
            Date date = new Date(millis);
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss,SSS");
            return format.format(date);
        }
    }
}
