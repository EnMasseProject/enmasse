/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.KubeUtil;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.stream.Stream.concat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class KubeUtilTest {

    private static final EnvVar FOO1_ENVVAR = new EnvVarBuilder().withName("FOO1").withValue("BAR").build();
    private static final EnvVar FOO1_UPD_ENVVAR = new EnvVarBuilder().withName("FOO1").withValue("BAZ").build();
    private static final EnvVar FOO2_ENVAR = new EnvVarBuilder().withName("FOO2").withValue("BAR").build();
    private static final EnvVar FOO3_ENVVAR = new EnvVarBuilder().withName("FOO3").withValue("BAX").build();

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

    @Test
    public void appliesContainerResourcesToPodTemplate() {
        Container actualContainer = new ContainerBuilder()
                .withName("foo").build();
        Map<String, Quantity> widgets = Collections.singletonMap("widgets", new QuantityBuilder().withAmount("10").build());
        ResourceRequirements resources = new ResourceRequirementsBuilder().withLimits(widgets).build();
        Container desiredContainer = new ContainerBuilder()
                .withName("foo")
                .withResources(resources).build();

        PodTemplateSpec actual = doApplyContainers(actualContainer, desiredContainer);

        Container container = actual.getSpec().getContainers().get(0);
        assertThat(container.getResources(), equalTo(resources));
    }

    @Test
    public void appliesContainerOrderIgnored() {
        Container actualFooContainer = new ContainerBuilder()
                .withName("foo").build();
        Container actualBarContainer = new ContainerBuilder()
                .withName("bar").build();

        Map<String, Quantity> widgets = Collections.singletonMap("widgets", new QuantityBuilder().withAmount("10").build());
        ResourceRequirements resources = new ResourceRequirementsBuilder().withLimits(widgets).build();
        Container desiredFooContainer = new ContainerBuilder()
                .withName("foo")
                .withResources(resources).build();

        PodTemplateSpec actual1 = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addToContainers(actualBarContainer, actualFooContainer)
                .endSpec()
                .build();

        PodTemplateSpec desired = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addToContainers(desiredFooContainer)
                .endSpec()
                .build();

        KubeUtil.applyPodTemplate(actual1, desired);

        PodTemplateSpec actual = actual1;

        Container barContainer = actual.getSpec().getContainers().get(0);
        assertThat(barContainer.getName(), equalTo("bar"));
        assertThat(barContainer.getResources(), nullValue());

        Container fooContainer = actual.getSpec().getContainers().get(1);
        assertThat(fooContainer.getName(), equalTo("foo"));
        assertThat(fooContainer.getResources(), equalTo(resources));
    }

    @Test
    public void appliesContainerEnvVarToPodTemplate() {
        Container actualContainer = new ContainerBuilder()
                .withName("foo")
                .withEnv(FOO1_ENVVAR, FOO2_ENVAR).build();
        Container desiredContainer = new ContainerBuilder()
                .withName("foo")
                .withEnv(FOO1_UPD_ENVVAR, FOO3_ENVVAR).build();

        PodTemplateSpec actual = doApplyContainers(actualContainer, desiredContainer);

        Container container = actual.getSpec().getContainers().get(0);
        List<EnvVar> probe = container.getEnv();
        assertThat(probe.size(), equalTo(3));
        assertThat(probe, containsInAnyOrder(FOO1_UPD_ENVVAR, FOO2_ENVAR, FOO3_ENVVAR));
    }

    @Test
    public void appliesInitContainerEnvVarToPodTemplate() {
        Container actualContainer = new ContainerBuilder()
                .withName("foo")
                .withEnv(FOO1_ENVVAR).build();
        Container desiredContainer = new ContainerBuilder()
                .withName("foo")
                .withEnv(FOO1_UPD_ENVVAR).build();

        PodTemplateSpec actual = doApplyInitContainers(actualContainer, desiredContainer);

        Container container = actual.getSpec().getInitContainers().get(0);
        List<EnvVar> probe = container.getEnv();
        assertThat(probe.size(), equalTo(1));
        assertThat(probe, containsInAnyOrder(FOO1_UPD_ENVVAR));
    }

    @Test
    public void appliesContainerLivenessProbeSettingsToPodTemplate() {
        Container actualContainer = new ContainerBuilder()
                .withName("foo")
                .withLivenessProbe(new ProbeBuilder()
                        .withInitialDelaySeconds(1)
                        .withPeriodSeconds(2)
                        .withFailureThreshold(4).build()).build();
        Container desiredContainer = new ContainerBuilder()
                .withName("foo")
                .withLivenessProbe(new ProbeBuilder()
                        .withInitialDelaySeconds(10)
                        .withSuccessThreshold(80).build()).build();
        PodTemplateSpec actual = doApplyContainers(actualContainer, desiredContainer);

        Container container = actual.getSpec().getContainers().get(0);
        Probe probe = container.getLivenessProbe();
        assertThat(probe.getInitialDelaySeconds(), equalTo(10));
        assertThat(probe.getPeriodSeconds(), equalTo(2));
        assertThat(probe.getFailureThreshold(), equalTo(4));
        assertThat(probe.getSuccessThreshold(), equalTo(80));
    }

    @Test
    public void appliesContainerReadinessProbeSettingsToPodTemplate() {
        Container actualContainer = new ContainerBuilder()
                .withName("foo")
                .withReadinessProbe(new ProbeBuilder()
                        .withInitialDelaySeconds(1).build()).build();
        Container desiredContainer = new ContainerBuilder()
                .withName("foo")
                .withReadinessProbe(new ProbeBuilder()
                        .withInitialDelaySeconds(10).build()).build();
        PodTemplateSpec actual = doApplyContainers(actualContainer, desiredContainer);

        Container container = actual.getSpec().getContainers().get(0);
        assertThat(container.getReadinessProbe().getInitialDelaySeconds(), equalTo(10));
    }

    private PodTemplateSpec doApplyContainers(Container actualContainer, Container desiredContainer) {
        PodTemplateSpec actual = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addToContainers(actualContainer)
                .endSpec()
                .build();

        PodTemplateSpec desired = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addToContainers(desiredContainer)
                .endSpec()
                .build();

        KubeUtil.applyPodTemplate(actual, desired);

        return actual;
    }

    private PodTemplateSpec doApplyInitContainers(Container actualContainer, Container desiredContainer) {
        PodTemplateSpec actual = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addToInitContainers(actualContainer)
                .endSpec()
                .build();

        PodTemplateSpec desired = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addToInitContainers(desiredContainer)
                .endSpec()
                .build();

        KubeUtil.applyPodTemplate(actual, desired);

        return actual;
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

        assertThat(result.length(), lessThanOrEqualTo(60));
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

    protected static Stream<Arguments> uuidNames() {
        return Stream.of(
                // standard test
                of("foo-bar", "0123456789012-3456-7890-123456789012", "foo-bar-0123456789012-3456-7890-123456789012"),
                // exceeds max length
                of("01234567890123456789012345678901234567890123456789", "0123456789012-3456-7890-123456789012", "01234567890123456789012-0123456789012-3456-7890-123456789012"),
                // exceeds max length, corner case, with dash
                of("0123456789012345678901-345678901234567890123456789", "0123456789012-3456-7890-123456789012", "0123456789012345678901--0123456789012-3456-7890-123456789012"),
                // exceeds max length, corner case, all dashy
                of("-----------------------345678901234567890123456789", "0123456789012-3456-7890-123456789012", "1-----------------------0123456789012-3456-7890-123456789012")
                );
    }

    @ParameterizedTest
    @MethodSource("uuidNames")
    public void testSanitizeUuidName(final String name, final String uuid, final String output) {
        assertEquals(output, KubeUtil.sanitizeWithUuid(name, uuid));
    }

    /**
     * Test for encoding with Go logic.
     */
    @Test
    public void testSanitizeForGo () {
        assertEquals("as1.telemetryiot-project-ns-4cf002ae-37a5-38d6-9749-b7f85b29a385", KubeUtil.sanitizeForGo("as1", "telemetry/iot-project-ns.iot1"));
    }

}
