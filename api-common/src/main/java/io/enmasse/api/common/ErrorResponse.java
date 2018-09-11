/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonProperty("apiVersion")
    private final String apiVersion = "v1";

    @JsonProperty("kind")
    private final String kind = "Status";

    @JsonProperty("status")
    private final String status = "Failure";

    @JsonProperty("code")
    private final int code;

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("message")
    private final String message;

    public ErrorResponse(int statusCode, String reason, String message) {
        this.code = statusCode;
        this.reason = reason;
        this.message = message;
    }

    public String getReason() {
        return reason;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return code;
    }
}
