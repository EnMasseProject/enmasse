/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.x509;

import io.enmasse.systemtest.iot.DeviceCertificateManager;
import io.enmasse.systemtest.iot.DeviceCertificateManager.Mode;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.http.StandardIoTHttpTests;
import io.enmasse.systemtest.iot.mqtt.StandardIoTMqttTests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import javax.security.auth.x500.X500Principal;

import static io.enmasse.systemtest.framework.TestTag.IOT;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.HTTP;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.MQTT;
import static io.enmasse.systemtest.utils.TestUtils.toPem;

@Tag(IOT)
public class X509EcAuthenticationTest implements StandardX509Cases, StandardIoTHttpTests, StandardIoTMqttTests {

    private static DeviceCertificateManager certificateManager;
    private static IoTTestSession session;

    @BeforeAll
    public static void setup() throws Exception {

        certificateManager = new DeviceCertificateManager(Mode.EC, new X500Principal("OU=Tenant 1,OU=IoT,O=EnMasse,C=IO"));

        session = IoTTestSession.createDefault()
                .tenant(tenant -> tenant.editOrNewSpec()
                        .editOrNewConfiguration()
                        .addNewTrustAnchor()
                        .withCertificate(toPem(certificateManager.getCertificate()))
                        .endTrustAnchor()
                        .endConfiguration()
                        .endSpec())
                .adapters(MQTT, HTTP)
                .deploy();

    }

    @AfterAll
    public static void cleanup() throws Exception {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    @Override
    public DeviceCertificateManager getCertificateManager() {
        return certificateManager;
    }

    @Override
    public IoTTestSession getSession() {
        return session;
    }

}
