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

    private String averageThroughput;
    private String totalThroughput;

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

    public String getTotalThroughput() {
        return totalThroughput;
    }

    public void setTotalThroughput(String totalThroughput) {
        this.totalThroughput = totalThroughput;
    }

    public String getAverageThroughput() {
        return averageThroughput;
    }

    public void setAverageThroughput(String averageThroughput) {
        this.averageThroughput = averageThroughput;
    }
}
