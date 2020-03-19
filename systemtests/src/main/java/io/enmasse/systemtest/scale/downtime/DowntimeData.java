/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.downtime;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DowntimeData {

    private String name;
    private List<String> reconnectTimes99p;
    private List<String> reconnectTimesMedian;
    private String globalReconnectTimes99pMedian;
    private String globalReconnectTimesMediansMedian;
    private String reconnectTimeAverage;
    private String createAddressTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getReconnectTimes99p() {
        return reconnectTimes99p;
    }

    public void setReconnectTimes99p(List<String> reconnectTimes99p) {
        this.reconnectTimes99p = reconnectTimes99p;
    }

    public List<String> getReconnectTimesMedian() {
        return reconnectTimesMedian;
    }

    public void setReconnectTimesMedian(List<String> reconnectTimesMedian) {
        this.reconnectTimesMedian = reconnectTimesMedian;
    }

    public String getGlobalReconnectTimes99pMedian() {
        return globalReconnectTimes99pMedian;
    }

    public void setGlobalReconnectTimes99pMedian(String globalReconnectTimes99pMedian) {
        this.globalReconnectTimes99pMedian = globalReconnectTimes99pMedian;
    }

    public String getGlobalReconnectTimesMediansMedian() {
        return globalReconnectTimesMediansMedian;
    }

    public void setGlobalReconnectTimesMediansMedian(String globalReconnectTimesMediansMedian) {
        this.globalReconnectTimesMediansMedian = globalReconnectTimesMediansMedian;
    }

    public String getReconnectTimeAverage() {
        return reconnectTimeAverage;
    }

    public void setReconnectTimeAverage(String reconnectTimeAverage) {
        this.reconnectTimeAverage = reconnectTimeAverage;
    }

    public String getCreateAddressTime() {
        return createAddressTime;
    }

    public void setCreateAddressTime(String createAddressTime) {
        this.createAddressTime = createAddressTime;
    }

}
