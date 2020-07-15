/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static io.enmasse.systemtest.framework.TestTag.FRAMEWORK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(FRAMEWORK)
public class MessageSendTesterTest {

    @Test
    public void testFillingEmpty() {
        final JsonObject json = new JsonObject();
        var result = MessageSendTester.fillWithPayload(json, 1024);
        assertEquals(1024, result.length());
    }

    @Test
    public void testFillUtf8() {
        final JsonObject json = new JsonObject();
        json.put("some-utf-8", "\u1F926");
        var result = MessageSendTester.fillWithPayload(json, 1024);
        assertEquals(1024, result.length());
    }

    @Test
    public void testFillingNonEmpty() {

        final int fixedLength = "{\"init\":\"\"}".getBytes(StandardCharsets.UTF_8).length;

        for (int initialLength = fixedLength; initialLength < 1024; initialLength++) {

            final char init[] = new char[initialLength - fixedLength];
            Arrays.fill(init, 'b');
            final String initValue = String.valueOf(init);

            for (int expectedLength = initialLength + MessageSendTester.FIXED_JSON_EXTRA_SIZE + 1; expectedLength < 1024; expectedLength++) {

                final JsonObject json = new JsonObject();
                json.put("init", initValue);

                var result = MessageSendTester.fillWithPayload(json, expectedLength);
                assertEquals(expectedLength, result.length());

            }

        }

    }

}
