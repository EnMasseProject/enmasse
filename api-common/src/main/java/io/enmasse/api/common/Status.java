/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Status {

    @JsonProperty("apiVersion")
    private final String apiVersion = "v1";

    @JsonProperty("kind")
    private final String kind = "Status";

    @JsonProperty("status")
    private final String status;

    @JsonProperty("code")
    private final int code;

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("details")
    private final Map<String, String> details;

    private Status(String status, int statusCode, String reason, String message, Map<String, String> details) {
        this.status = status;
        this.code = statusCode;
        this.reason = reason;
        this.message = message;
        this.details = details;
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

    public static Status failureStatus(int statusCode, String reason, String message) {
        return new Status("Failure", statusCode, reason, message, null);
    }

    public static Status notFound(String kind, String name) {
        Map<String, String> details = new HashMap<>();
        details.put("kind", kind);
        details.put("name", name);
        return new Status("Failure", 404, "NotFound", kind + " " + name + " not found", details);
    }

    public static Status successStatus(int statusCode) {
        return new Status("Success", statusCode, null, null, null);
    }

    public static Status successStatus(int statusCode, String kind, String name, String uid) {
        Map<String, String> details = new HashMap<>();
        details.put("kind", kind);
        details.put("name", name);
        details.put("uid", uid);
        return new Status("Success", statusCode, null, null, details);
    }
}
