/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SQLTest {

    public static Stream<Arguments> typeDetectorValue() {
        return Stream.of(
                Arguments.of("jdbc:postgresql://localhost:1234/device-registry", "postgresql"),
                Arguments.of("jdbc:h2:~/test;ACCESS_MODE_DATA=rws", "h2"));
    }

    @ParameterizedTest
    @MethodSource("typeDetectorValue")
    public void testTypeDetector(final String url, final String expected) {
        assertEquals(expected, SQL.getDatabaseDialect(url));
    }

}
