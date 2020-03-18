/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import java.util.ArrayList;
import java.util.List;

public class ThroughputData {

    private List<String> throughputs99p = new ArrayList<>();
    private List<String> throughputsMedian = new ArrayList<>();
    private String globalThroughputs99pMedian;
    private String globalThroughputsMediansMedian;
    private List<String> msgPerSecond = new ArrayList<>();

    public List<String> getThroughputs99p() {
        return throughputs99p;
    }

    public void setThroughputs99p(List<String> throughputs99p) {
        this.throughputs99p = throughputs99p;
    }

    public List<String> getThroughputsMedian() {
        return throughputsMedian;
    }

    public void setThroughputsMedian(List<String> throughputsMedian) {
        this.throughputsMedian = throughputsMedian;
    }

    public String getGlobalThroughputs99pMedian() {
        return globalThroughputs99pMedian;
    }

    public void setGlobalThroughputs99pMedian(String globalThroughputs99pMedian) {
        this.globalThroughputs99pMedian = globalThroughputs99pMedian;
    }

    public String getGlobalThroughputsMediansMedian() {
        return globalThroughputsMediansMedian;
    }

    public void setGlobalThroughputsMediansMedian(String globalThroughputsMediansMedian) {
        this.globalThroughputsMediansMedian = globalThroughputsMediansMedian;
    }

    public List<String> getMsgPerSecond() {
        return msgPerSecond;
    }

    public void setMsgPerSecond(List<String> msgPerSecond) {
        this.msgPerSecond = msgPerSecond;
    }

}
