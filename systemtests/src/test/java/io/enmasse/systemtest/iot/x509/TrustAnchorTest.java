/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.x509;

import io.enmasse.iot.model.v1.IoTTenant;
import io.enmasse.iot.model.v1.IoTTenantStatus;
import io.enmasse.iot.model.v1.TenantConditionType;
import io.enmasse.systemtest.certs.ssl.SSLUtils;
import io.enmasse.systemtest.iot.DeviceCertificateManager;
import io.enmasse.systemtest.iot.DeviceCertificateManager.Mode;
import io.enmasse.systemtest.iot.IoTTestContext;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.IoTTests;
import io.enmasse.systemtest.iot.Names;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.time.Duration;
import java.util.Optional;

import static io.enmasse.systemtest.framework.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.framework.TestTag.IOT;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.HTTP;

@Tag(IOT)
public class TrustAnchorTest implements IoTTests {

    private static final Logger log = LoggerFactory.getLogger(TrustAnchorTest.class);

    /**
     * Test creating duplicate "subject dn".
     */
    @Test
    @Tag(ACCEPTANCE)
    @Disabled("Causes stability issues")
    public void testDuplicateSubjectDn() throws Exception {

        // create two CAs with the same name

        var mgr1 = new DeviceCertificateManager(Mode.RSA, new X500Principal("OU=Tenant 1,OU=IoT,O=EnMasse,C=IO"));
        var mgr2 = new DeviceCertificateManager(Mode.RSA, new X500Principal("OU=Tenant 1,OU=IoT,O=EnMasse,C=IO"));

        final String name2 = Names.randomName();
        final String ns2 = Names.randomNamespace();

        try (

                IoTTestSession session = IoTTestSession.createDefault()
                        .tenant(tenant -> tenant.editOrNewSpec()
                                .editOrNewConfiguration()
                                .addNewTrustAnchor()
                                .withCertificate(SSLUtils.toPem(mgr1.getCertificate()))
                                .endTrustAnchor()
                                .endConfiguration()
                                .endSpec())
                        .adapters(HTTP)
                        .deploy();

                IoTTestContext secondTenant = session
                        .newTenant(ns2, name2)
                        .createNamespace(true)
                        .awaitReady(false)
                        .tenant(tenant -> tenant.editOrNewSpec()
                                .editOrNewConfiguration()
                                .addNewTrustAnchor()
                                .withCertificate(SSLUtils.toPem(mgr2.getCertificate()))
                                .endTrustAnchor()
                                .endConfiguration()
                                .endSpec()
                        )
                        .endpoint(endpoint -> endpoint
                                .editOrNewSpec()
                                .withHost(null)
                                .endSpec())
                        .deploy()

        ) {

            // and wait for the condition

            TestUtils.waitUntilConditionOrFail(() -> {

                // wait until the condition 'TrustAnchorsUnique' becomes false
                return Optional
                        .ofNullable(secondTenant.tenant().get())
                        .map(IoTTenant::getStatus)
                        .map(IoTTenantStatus::getConditions)
                        .flatMap(c -> c.stream()
                                .filter(condition -> condition.getType() == TenantConditionType.TRUST_ANCHORS_UNIQUE)
                                .findAny())
                        .map(c -> "false".equalsIgnoreCase(c.getStatus()) &&
                                "DuplicateTrustAnchors".equals(c.getReason()))
                        .orElse(false);

            }, Duration.ofMinutes(5), Duration.ofSeconds(10), () -> "Condition 'TrustAnchorsUnique' should become 'false'");

            log.info("Successfully detected 'DuplicateTrustAnchors' condition on second tenant");

        }

    }

}
