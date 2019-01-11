/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.KubeUtil;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.stream.Stream;

import org.hamcrest.Matchers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.stream.Stream.concat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class KubeUtilTest {

    @Test
    public void testLeaveSpaceForPodIdentifier() {
        String address = "receiver-round-robincli_rhearatherlongaddresswhichcanbeverylongblablabla";
        String id = KubeUtil.sanitizeName(address);
        assertThat(id.length(), is(60));
        String id2 = KubeUtil.sanitizeName(id);
        assertThat(id, is(id2));
    }

    @Test
    public void testNull() {
        assertNull(KubeUtil.sanitizeName(null));
        assertNull(KubeUtil.sanitizeUserName(null));
    }

    protected static Stream<Arguments> commonNames() {
        return Stream.of(
                        of("foo", "foo"),
                        of("-foo", "1foo"),
                        of("foo-", "foo1"),
                        of("-foo-", "1foo1"),
                        of("--foo--", "1-foo-1"),
                        of("--f-o-o--", "1-f-o-o-1"),

                        of("foo#bar", "foobar"),
                        of("#foo#bar", "foobar"),
                        of("foo#bar#", "foobar"),
                        of("#foo#bar#", "foobar"),

                        of("foo##bar", "foobar"),
                        of("foo#&&#bar", "foobar"),
                        of("foo bar", "foobar"),

                        of("###", ""),

                        of("", "")

                        );
    }
    
    protected static Stream<Arguments> addressNames() {
        return concat(
                        commonNames(),
                        Stream.of(
                                        of("foo@bar", "foobar"),
                                        of("foo.bar", "foobar")
                                        ));
    }

    @ParameterizedTest
    @MethodSource("addressNames")
    public void testSanitizeAddressName(final String input, final String output) {
        final String result = KubeUtil.sanitizeName(input);

        assertThat(result.length(), Matchers.lessThanOrEqualTo(60));
        assertEquals(output, result);

    }

    protected static Stream<Arguments> userNames() {
        return concat(
                        commonNames(),
                        Stream.of(
                                        of("foo@bar", "foo@bar"),
                                        of("foo.bar", "foo.bar")
                                        ));
    }

    @ParameterizedTest
    @MethodSource("userNames")
    public void testSanitizeUserName(final String input, final String output) {
        assertEquals(output, KubeUtil.sanitizeUserName(input));
    }

}
