/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.concurrent.TimeUnit;

/**
 * Keeps track of a timeout value that will decrease as time goes.
 */
public class TimeoutBudget {
    private long startTime;
    private long endTime;

    public TimeoutBudget(long timeout, TimeUnit timeUnit) {
        reset(timeout, timeUnit);
    }

    private void reset(long timeout, TimeUnit timeUnit) {
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + timeUnit.toMillis(timeout);
    }

    public long timeLeft() {
        long diff = endTime - System.currentTimeMillis();
        if (diff >= 0) {
            return diff;
        } else {
            return -1;
        }
    }
}
