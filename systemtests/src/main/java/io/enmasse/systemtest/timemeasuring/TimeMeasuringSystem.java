/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.timemeasuring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Environment;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class TimeMeasuringSystem {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss:SSS");
    private static final Logger log = CustomLogger.getLogger();
    private static TimeMeasuringSystem instance;
    private Map<String, Map<String, Map<String, MeasureRecord>>> measuringMap;
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
        String id = operation.toString();
        if (!operation.equals(Operation.TEST_EXECUTION)) {
            id = String.format("%s-%s", id, UUID.randomUUID().toString().split("-")[0]);
        }
        return id;
    }

    private void addRecord(String operationID, MeasureRecord record) {
        if (measuringMap.get(testClass) == null) {
            LinkedHashMap<String, Map<String, MeasureRecord>> newData = new LinkedHashMap<>();
            LinkedHashMap<String, MeasureRecord> newRecord = new LinkedHashMap<>();
            newData.put(testName, newRecord);
            newRecord.put(operationID, record);
            measuringMap.put(testClass, newData);
        } else if (measuringMap.get(testClass).get(testName) == null){
            LinkedHashMap<String, MeasureRecord> newRecord = new LinkedHashMap<>();
            newRecord.put(operationID, record);
            measuringMap.get(testClass).put(testName, newRecord);
        } else {
            measuringMap.get(testClass).get(testName).put(operationID, record);
        }
    }

    private String setStartTime(Operation operation) {
        String id = createOperationsID(operation);
        try {
            addRecord(id, new MeasureRecord(System.currentTimeMillis()));
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
            measuringMap.get(testClass).get(testName).get(id).setEndTime(System.currentTimeMillis());
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
        measuringMap.forEach((testClassID, testClassRecords) -> {
            log.info("================================================");
            log.info("================================================");
            log.info(testClassID);
            testClassRecords.forEach((testID, testRecord) -> {
                log.info("---------------------------------------------");
                log.info(testID);
                testRecord.forEach((operationID, record) -> {
                    log.info("Operation id: {} duration: {} started: {} ended: {}",
                            operationID,
                            record.getDurationReadable(),
                            record.getStartTimeHumanReadable(),
                            record.getEndTimeHumanReadable());
                });
            });
        });
    }

    private void saveResults() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(measuringMap);
            Environment env = new Environment();
            Date timestamp = new Date(System.currentTimeMillis());
            Path logPath = Paths.get(env.testLogDir(), "timeMeasuring");
            Files.createDirectories(logPath);
            Files.write(Paths.get(logPath.toString(),
                    String.format("duration_report-%s.json", dateFormat.format(timestamp))), json.getBytes());
        } catch (Exception ex) {
            log.warn("Cannot save output of time measuring: " + ex.getMessage());
        }

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
        private long startTime;
        private long endTime;
        private long duration;
        private String durationReadable;

        MeasureRecord(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = endTime - startTime;
            this.durationReadable = getDurationHumanReadable();
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

        long getDuration() {
            return this.duration;
        }

        String getDurationReadable() {
            return this.durationReadable;
        }

        void setEndTime(long endTime) {
            this.endTime = endTime;
            this.duration = endTime - startTime;
            this.durationReadable = getDurationHumanReadable();
        }

        String getDurationHumanReadable() {
            long millis = getDuration();
            long hours = TimeUnit.MILLISECONDS.toHours(millis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - (TimeUnit.MILLISECONDS.toHours(millis) * 60);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - (TimeUnit.MILLISECONDS.toMinutes(millis) * 60);
            long milliseconds = TimeUnit.MILLISECONDS.toMillis(millis) - (TimeUnit.MILLISECONDS.toSeconds(millis) * 1000);

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
