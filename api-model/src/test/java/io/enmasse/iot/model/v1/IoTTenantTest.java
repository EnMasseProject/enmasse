/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IoTTenantTest {

    @Test
    void testCreateManaged() {
        IoTTenant project = new IoTTenantBuilder()
                .withNewMetadata()
                .withName("proj")
                .endMetadata()
                .withNewSpec()
                .withNewDownstream()
                .endDownstream()
                .endSpec()
                .build();

        assertEquals("proj", project.getMetadata().getName());
    }

    @Test
    public void testParse() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final URL url = IoTTenantTest.class.getResource("resources/simple-project.json");
        final IoTTenant value = mapper.readValue(url, IoTTenant.class);

        assertThat(value, notNullValue());

        assertThat(value.getMetadata(), notNullValue());
        assertThat(value.getMetadata().getName(), is("iot"));

        assertThat(value.getSpec(), notNullValue());

        assertThat(value.getSpec().getDownstream(), notNullValue());
    }

}
