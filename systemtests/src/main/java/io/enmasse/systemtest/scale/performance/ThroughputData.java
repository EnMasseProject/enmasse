/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.performance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.List;

@JsonInclude(Include.NON_NULL)
 public class ThroughputData {

    private String name;

    private List<String> msgPerSecond;

    private String perClientThroughput99p;
    private String perClientThroughputMedian;
    private String estimateTotalThroughput99p;
    private String estimateTotalThroughputMedian;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMsgPerSecond() {
        return msgPerSecond;
    }

    public void setMsgPerSecond(List<String> msgPerSecond) {
        this.msgPerSecond = msgPerSecond;
    }

    public String getPerClientThroughput99p() {
        return perClientThroughput99p;
    }

    public void setPerClientThroughput99p(String perClientThroughput99p) {
        this.perClientThroughput99p = perClientThroughput99p;
    }

    public String getPerClientThroughputMedian() {
        return perClientThroughputMedian;
    }

    public void setPerClientThroughputMedian(String perClientThroughputMedian) {
        this.perClientThroughputMedian = perClientThroughputMedian;
    }

    public String getEstimateTotalThroughput99p() {
        return estimateTotalThroughput99p;
    }

    public void setEstimateTotalThroughput99p(String estimateTotalThroughput99p) {
        this.estimateTotalThroughput99p = estimateTotalThroughput99p;
    }

    public String getEstimateTotalThroughputMedian() {
        return estimateTotalThroughputMedian;
    }

    public void setEstimateTotalThroughputMedian(String estimateTotalThroughputMedian) {
        this.estimateTotalThroughputMedian = estimateTotalThroughputMedian;
    }

}
