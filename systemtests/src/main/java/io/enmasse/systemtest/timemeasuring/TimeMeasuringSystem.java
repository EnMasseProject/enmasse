/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.timemeasuring;

import io.enmasse.systemtest.*;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class TimeMeasuringSystem {
    private static final Logger log = CustomLogger.getLogger();
    private static TimeMeasuringSystem instance;
    private Map<String, TestTimeDuration> measuringMap;
    private String testName;
    private int operationCounter;


    //===============================================================
    // private instance methods
    //===============================================================
    private TimeMeasuringSystem() {
        measuringMap = new HashMap<>();
    }

    private static synchronized TimeMeasuringSystem getInstance() {
        if (instance == null) {
            instance = new TimeMeasuringSystem();
        }
        return instance;
    }

    private void setStartTime(Operation operationID) {
        String id = String.format("%s-%s", testName, operationID);
        if (measuringMap.get(id) != null) {
            id = String.format("%s-%s", id, ++operationCounter);
        }
        measuringMap.put(id, new TestTimeDuration(System.currentTimeMillis()));
    }

    private void setEndTime(Operation operationID) {
        measuringMap.get(String.format("%s-%s", testName, operationID)).setEndTime(System.currentTimeMillis());
    }

    private void serTestNameID(String id) {
        this.operationCounter = 0;
        this.testName = id;
    }

    private void printAndSaveResults() {
        measuringMap.forEach((id, timeObject) -> log.info("Operation id: {} duration: {}", id, timeObject.getDurationHumanReadable()));
    }

    //===============================================================
    // public static methods
    //===============================================================
    public static void setTestName(String name) {
        TimeMeasuringSystem.getInstance().serTestNameID(name);
    }

    public static void startOperation(Operation operationID) {
        TimeMeasuringSystem.getInstance().setStartTime(operationID);
    }

    public static void stopOperation(Operation operationID) {
        TimeMeasuringSystem.getInstance().setEndTime(operationID);
    }

    public static void printResults() {
        TimeMeasuringSystem.getInstance().printAndSaveResults();
    }

    /**
     * Test time duration class
     */
    private class TestTimeDuration {
        long startTime;
        long endTime;

        public TestTimeDuration(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public TestTimeDuration(long startTime) {
            this.startTime = startTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public long getDuration() {
            return endTime - startTime;
        }

        public String getDurationHumanReadable() {
            long milis = getDuration();
            long hours = TimeUnit.MILLISECONDS.toHours(milis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(milis) - (TimeUnit.MILLISECONDS.toHours(milis) * 60);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(milis) - (TimeUnit.MILLISECONDS.toMinutes(milis) * 60);
            long milliseconds = TimeUnit.MILLISECONDS.toMillis(milis) - (TimeUnit.MILLISECONDS.toSeconds(milis) * 1000);

            return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds);
        }
    }
}
