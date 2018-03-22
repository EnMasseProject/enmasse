/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

class StatusCheckResult {
    private final boolean success;
    private final boolean allowSuccessForPooled;
    private final String message;

    StatusCheckResult(boolean success) {
        this(success, null);
    }

    StatusCheckResult(boolean success, String message) {
        this(success, false, message);
    }

    StatusCheckResult(boolean success, boolean allowSuccessForPooled, String message) {
        this.success = success;
        this.allowSuccessForPooled = allowSuccessForPooled;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean allowSuccessForPooled() {
        return allowSuccessForPooled;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "{success=" + success + ", allowSuccessForPooled=" + allowSuccessForPooled + ", message=" + message;
    }
}
