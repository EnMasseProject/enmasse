/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class IoTProjectTest {

    @Test
    void testCreateExternal() {
        IoTProject project = new IoTProjectBuilder()
                .withNewMetadata()
                .withName("proj")
                .endMetadata()
                .withNewSpec()
                .withNewDownstreamStrategy()
                .withNewExternalStrategy()
                .withCertificate(ByteBuffer.wrap("a".getBytes()))
                .withHost("host")
                .withPassword("pwd")
                .withUsername("username")
                .withPort(1234)
                .withTls(true)
                .endExternalStrategy()
                .endDownstreamStrategy()
                .endSpec()
                .build();

        assertEquals("proj", project.getMetadata().getName());
        assertArrayEquals("a".getBytes(), project.getSpec().getDownstreamStrategy().getExternalStrategy().getCertificate().array());
        assertEquals("host", project.getSpec().getDownstreamStrategy().getExternalStrategy().getHost());
        assertEquals("pwd", project.getSpec().getDownstreamStrategy().getExternalStrategy().getPassword());
        assertEquals("username", project.getSpec().getDownstreamStrategy().getExternalStrategy().getUsername());
        assertEquals(1234, project.getSpec().getDownstreamStrategy().getExternalStrategy().getPort());
        assertEquals(true, project.getSpec().getDownstreamStrategy().getExternalStrategy().isTls());
        assertNull(project.getSpec().getDownstreamStrategy().getManagedStrategy());
    }

    @Test
    void testCreateManaged() {
        IoTProject project = new IoTProjectBuilder()
                .withNewMetadata()
                .withName("proj")
                .endMetadata()
                .withNewSpec()
                .withNewDownstreamStrategy()
                .withNewManagedStrategy()
                .withNewAddressSpace()
                .withName("managed")
                .endAddressSpace()
                .endManagedStrategy()
                .endDownstreamStrategy()
                .endSpec()
                .build();

        assertEquals("proj", project.getMetadata().getName());
        assertEquals("managed", project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());
        assertNull(project.getSpec().getDownstreamStrategy().getExternalStrategy());
    }

    @Test
    public void testParse() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final URL url = IoTProjectTest.class.getResource("resources/external-project.json");
        final IoTProject value = mapper.readValue(url, IoTProject.class);

        assertThat(value, notNullValue());

        assertThat(value.getMetadata(), notNullValue());
        assertThat(value.getMetadata().getName(), is("iot"));

        assertThat(value.getSpec(), notNullValue());

        assertThat(value.getSpec().getDownstreamStrategy(), notNullValue());
        assertThat(value.getSpec().getDownstreamStrategy().getExternalStrategy(), notNullValue());
        assertThat(value.getSpec().getDownstreamStrategy().getExternalStrategy().getCertificate(), notNullValue());
        assertThat(value.getSpec().getDownstreamStrategy().getExternalStrategy().getCertificate().remaining(), is(1220));
    }

}
