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
    private Map<String, Map<String, MeasureRecord>> measuringMap;
    private String testClass;
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

    private void addRecord(String testClassID, String operationID, MeasureRecord record) {
        if (measuringMap.get(testClassID) == null) {
            LinkedHashMap<String, MeasureRecord> newData = new LinkedHashMap<>();
            newData.put(operationID, record);
            measuringMap.put(testClassID, newData);
        } else {
            measuringMap.get(testClassID).put(operationID, record);
        }
    }

    private String setStartTime(Operation operation) {
        String id = createOperationsID(operation);
        try {
            addRecord(testClass, id, new MeasureRecord(System.currentTimeMillis()));
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
            measuringMap.get(testClass).get(id).setEndTime(System.currentTimeMillis());
            log.info("End time of operation {} is correctly stored", id);
        } catch (Exception ex) {
            log.warn("End time of operation {} is not set due to exception", id);
        }
    }

    private void setTestName(String testName) {
        this.testName = testName;
    }

    private void setTestClass(String testClass) {
        this.testClass = testClass;
    }

    private void printResults() {
        for (Map.Entry<String, Map<String, MeasureRecord>> baseRecord : measuringMap.entrySet()) {
            log.info("================================================");
            log.info("================================================");
            log.info(baseRecord.getKey());
            String tmpID = "";
            for (Map.Entry<String, MeasureRecord> record : baseRecord.getValue().entrySet()) {
                if (!record.getKey().contains(tmpID)) {
                    log.info("---------------------------------------------");
                }
                log.info("Operation id: {} duration: {} started: {} ended: {}",
                        record.getKey(),
                        record.getValue().getDurationHumanReadable(),
                        record.getValue().getStartTimeHumanReadable(),
                        record.getValue().getEndTimeHumanReadable());
                tmpID = record.getKey().split("-")[0];
            }
        }
    }

    private void saveResults() {

    }

    //===============================================================
    // public static methods
    //===============================================================
    public static void setTestName(String testClass, String testName) {
        TimeMeasuringSystem.getInstance().setTestName(testName);
        TimeMeasuringSystem.getInstance().setTestClass(testClass);
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

    public static void printAndSaveResults() {
        TimeMeasuringSystem.getInstance().printResults();
        TimeMeasuringSystem.getInstance().saveResults();
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
