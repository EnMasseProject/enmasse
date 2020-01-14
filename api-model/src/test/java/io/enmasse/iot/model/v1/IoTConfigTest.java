/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class IoTConfigTest {

    @Test
    void testCreateConfig() {
        IoTConfig config = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("config")
                .endMetadata()
                .withNewSpec()
                .withNewInterServiceCertificates()
                .withNewSecretCertificatesStrategy()
                .withCaSecretName("ca-secret-name")
                .withServiceSecretNames(Collections.singletonMap("iot", "value"))
                .endSecretCertificatesStrategy()
                .endInterServiceCertificates()
                .withImageOverrides(Collections.singletonMap("image1",
                        new ImageOverrideBuilder()
                            .withName("img")
                            .withImagePullPolicy("Always")
                            .build()))
                .endSpec()
                .build();

        assertEquals("config", config.getMetadata().getName());
        assertNotNull(config.getSpec().getInterServiceCertificates());
        assertNotNull(config.getSpec().getInterServiceCertificates().getSecretCertificatesStrategy());
        assertEquals("ca-secret-name",
                config.getSpec().getInterServiceCertificates().getSecretCertificatesStrategy().getCaSecretName());
        assertEquals(Collections.singletonMap("iot", "value"),
                config.getSpec().getInterServiceCertificates().getSecretCertificatesStrategy().getServiceSecretNames());
        assertNotNull(config.getSpec().getImageOverrides());
        ImageOverride image = config.getSpec().getImageOverrides().get("image1");
        assertNotNull(image);
        assertEquals("img", image.getName());
        assertEquals("Always", image.getImagePullPolicy());
        assertNull(config.getSpec().getEnableDefaultRoutes());
    }

    @Test
    public void testParse() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final URL url = IoTConfigTest.class.getResource("resources/iot-config.json");
        final IoTConfig value = mapper.readValue(url, IoTConfig.class);

        assertThat(value, notNullValue());

        assertThat(value.getMetadata(), notNullValue());
        assertThat(value.getMetadata().getName(), is("default"));

        assertThat(value.getSpec(), notNullValue());

        assertThat(value.getSpec().getEnableDefaultRoutes(), notNullValue());
        assertThat(value.getSpec().getEnableDefaultRoutes(), is(true));

        assertThat(value.getSpec().getImageOverrides(), notNullValue());
        assertThat(value.getSpec().getImageOverrides().get("image1"), notNullValue());
        assertThat(value.getSpec().getImageOverrides().get("image2"), notNullValue());
        assertThat(value.getSpec().getImageOverrides().get("image1").getName(), is("image1-name"));
        assertThat(value.getSpec().getImageOverrides().get("image1").getImagePullPolicy(), is("Always"));

        assertThat(value.getSpec().getInterServiceCertificates(), notNullValue());
        assertThat(value.getSpec().getInterServiceCertificates().getSecretCertificatesStrategy(), notNullValue());
        assertThat(value.getSpec().getInterServiceCertificates().getSecretCertificatesStrategy().getCaSecretName(), is("ca-name"));
        assertThat(value.getSpec().getInterServiceCertificates().getSecretCertificatesStrategy().getServiceSecretNames(), notNullValue());
        assertThat(value.getSpec().getInterServiceCertificates().getSecretCertificatesStrategy().getServiceSecretNames().get("secret1"), is("value1"));
        assertThat(value.getSpec().getInterServiceCertificates().getSecretCertificatesStrategy().getServiceSecretNames().get("secret2"), is("value2"));

        assertThat(value.getStatus(), notNullValue());
        assertThat(value.getStatus().getPhase(), is("Ready"));
        assertThat(value.getStatus().getAuthenticationServicePSK(), is("psk"));

    }

}
