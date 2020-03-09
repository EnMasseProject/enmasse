/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

public class CreationTimeItem {

    int batchNumber;
    double timeToCreateSeconds;
    double timeToGetReadySeconds;

    public CreationTimeItem() {
        // empty
    }

    public CreationTimeItem(int batchNumber, double timeToCreateSeconds, double timeToGetReadySeconds) {
        this.batchNumber = batchNumber;
        this.timeToCreateSeconds = timeToCreateSeconds;
        this.timeToGetReadySeconds = timeToGetReadySeconds;
    }

    public int getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(int batchNumber) {
        this.batchNumber = batchNumber;
    }

    public double getTimeToCreateSeconds() {
        return timeToCreateSeconds;
    }

    public void setTimeToCreateSeconds(double timeToCreateSeconds) {
        this.timeToCreateSeconds = timeToCreateSeconds;
    }

    public double getTimeToGetReadySeconds() {
        return timeToGetReadySeconds;
    }

    public void setTimeToGetReadySeconds(double timeToGetReadySeconds) {
        this.timeToGetReadySeconds = timeToGetReadySeconds;
    }

    @Override
    public String toString() {
        return "{batchNumber:" + batchNumber + ", timeToCreateSeconds:" + timeToCreateSeconds + ", timeToGetReadySeconds:" + timeToGetReadySeconds + "}";
    }

}
