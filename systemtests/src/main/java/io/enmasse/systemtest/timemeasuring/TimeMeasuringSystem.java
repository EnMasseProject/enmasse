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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
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

    private String createOperationsID(SystemtestsOperation operation) {
        String id = operation.toString();
        if (!operation.equals(SystemtestsOperation.TEST_EXECUTION)) {
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
        } else if (measuringMap.get(testClass).get(testName) == null) {
            LinkedHashMap<String, MeasureRecord> newRecord = new LinkedHashMap<>();
            newRecord.put(operationID, record);
            measuringMap.get(testClass).put(testName, newRecord);
        } else {
            measuringMap.get(testClass).get(testName).put(operationID, record);
        }
    }

    private String setStartTime(SystemtestsOperation operation) {
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
        if (id.equals(SystemtestsOperation.TEST_EXECUTION.toString())) {
            id = createOperationsID(SystemtestsOperation.TEST_EXECUTION);
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

    private void saveResults(Map<?,?> data, String name) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(data);
            Date timestamp = new Date(System.currentTimeMillis());
            Path logPath = Paths.get(Environment.getInstance().testLogDir(), "timeMeasuring");
            Files.createDirectories(logPath);
            Files.write(Paths.get(logPath.toString(),
                    String.format("%s-%s.json", name, dateFormat.format(timestamp))), json.getBytes());
        } catch (Exception ex) {
            log.warn("Cannot save output of time measuring: " + ex.getMessage());
        }

    }

    private Map<String, Long> getSumDuration() {
        Map<String, Long> sumData = new LinkedHashMap<>();
        Arrays.stream(SystemtestsOperation.values()).forEach(value -> sumData.put(value.toString(), (long) 0));

        measuringMap.forEach((testClassID, testClassRecords) -> testClassRecords.forEach((testID, testRecord) -> {
            testRecord.forEach((operationID, record) -> {
                String key = operationID.split("-")[0];
                sumData.put(key, sumData.get(key) + record.duration);
            });
        }));

        return sumData;
    }

    private void saveCsvResults(Map<String, Long> data, String name) {
        Path logPath = Paths.get(Environment.getInstance().testLogDir(), "timeMeasuring");
        Path filePath = Paths.get(logPath.toString(), String.format("%s.csv", name));
        Map<String, Long> loadedData = null;
        List<String[]> csvData = new LinkedList<>();

        //Check if csv file already exists, if yes get data
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(filePath.toString()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                csvData.add(line.split(","));
            }
            loadedData = new LinkedHashMap<>();
            for (int i = 0; i < csvData.get(0).length; i++) {
                loadedData.put(csvData.get(0)[i], Long.parseLong(csvData.get(1)[i]));
            }
        } catch (Exception ex) {
            log.warn("Cannot load data from previous csv file");
        }

        try {
            Files.createDirectories(logPath);
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                writer.write(String.join(",", data.keySet().toArray(new String[0])));
                writer.newLine();
                if (loadedData != null) {
                    loadedData.forEach((operation, duration) -> {
                        data.put(operation, data.getOrDefault(operation, 0L) + duration);
                    });
                }
                writer.write(String.join(",", data.values().stream().map(value -> Long.toString(value)).toArray(String[]::new)));
                writer.newLine();
            }
        } catch (IOException ex) {
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

    public static String startOperation(SystemtestsOperation operation) {
        return TimeMeasuringSystem.getInstance().setStartTime(operation);
    }

    public static void stopOperation(String operationId) {
        TimeMeasuringSystem.getInstance().setEndTime(operationId);
    }

    public static void stopOperation(SystemtestsOperation operationId) {
        TimeMeasuringSystem.stopOperation(operationId.toString());
    }

    public static void printAndSaveResults() {
        Map<String, Long> sumData = TimeMeasuringSystem.getInstance().getSumDuration();
        TimeMeasuringSystem.getInstance().printResults();
        TimeMeasuringSystem.getInstance().saveResults(TimeMeasuringSystem.getInstance().measuringMap, "duration_report");
        TimeMeasuringSystem.getInstance().saveResults(sumData, "duration_sum_report");
        TimeMeasuringSystem.getInstance().saveCsvResults(sumData, "duration_sum_report");
    }

    /**
     * Test time duration class
     */
    @SuppressWarnings("unused")
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
