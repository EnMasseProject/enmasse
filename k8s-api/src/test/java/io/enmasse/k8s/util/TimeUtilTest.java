/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TimeUtilTest {
    @Test
    public void testFormatter() {
        assertEquals("1970-01-01T00:00:00Z", TimeUtil.formatRfc3339(Instant.ofEpochSecond(0)));
    }

    @Test
    public void testParser() {
        assertEquals(1543313902, TimeUtil.parseRfc3339("2018-11-27T10:18:22Z").getEpochSecond());
    }

    @Test
    public void testConvertToDuration() {
        assertEquals("<invalid>", TimeUtil.formatHumanReadable(Duration.ofSeconds(-1)));
        assertEquals("10s", TimeUtil.formatHumanReadable(Duration.ofSeconds(10)));
        assertEquals("10m", TimeUtil.formatHumanReadable(Duration.ofMinutes(10)));
        assertEquals("10h", TimeUtil.formatHumanReadable(Duration.ofHours(10)));
        assertEquals("10d", TimeUtil.formatHumanReadable(Duration.ofDays(10)));
        assertEquals("1y", TimeUtil.formatHumanReadable(Duration.ofDays(400)));
    }
}
