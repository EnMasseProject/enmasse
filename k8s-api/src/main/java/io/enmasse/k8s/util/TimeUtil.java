/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimeUtil {
    private static final Logger log = LoggerFactory.getLogger(TimeUtil.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneId.of("UTC"));

    private static final DateTimeFormatter fallbackPattern = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"));


    public static String formatRfc3339(Instant instant) {
        return formatter.format(instant);
    }

    public static Instant parseRfc3339(String data) {
        try {
            return LocalDateTime.parse(data, formatter).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            log.info("Failed parsing {} using format '{}', falling back to format '{}'", data, formatter, fallbackPattern);
            return LocalDateTime.parse(data, fallbackPattern).toInstant(ZoneOffset.UTC);
        }
    }

    public static String formatHumanReadable(Duration duration) {
        if (duration.isNegative()) {
            return "<invalid>";
        } else if (duration.getSeconds() < 0) {
            return "0s";
        } else if (duration.getSeconds() < 60) {
            return String.format("%ds", duration.getSeconds());
        } else if (duration.toMinutes() < 60) {
            return String.format("%dm", duration.toMinutes());
        } else if (duration.toHours() < 24) {
            return String.format("%dh", duration.toHours());
        } else if (duration.toDays() < 365) {
            return String.format("%dd", duration.toDays());
        } else {
            return String.format("%dy", duration.toDays() / 365);
        }
    }
}
