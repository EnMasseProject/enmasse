/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

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

}
