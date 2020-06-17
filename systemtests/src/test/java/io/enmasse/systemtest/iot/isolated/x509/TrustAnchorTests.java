/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.x509;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.ISOLATED_IOT;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.HTTP;
import static io.enmasse.systemtest.utils.TestUtils.toPem;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.iot.model.v1.IoTProjectStatus;
import io.enmasse.iot.model.v1.ProjectConditionType;
import io.enmasse.systemtest.iot.DeviceCertificateManager;
import io.enmasse.systemtest.iot.DeviceCertificateManager.Mode;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.IoTTests;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;

@Tag(ISOLATED_IOT)
public class TrustAnchorTests implements IoTTests {

    private static final Logger log = LoggerFactory.getLogger(TrustAnchorTests.class);

    /**
     * Test creating duplicate "subject dn".
     */
    @Test
    @Tag(ACCEPTANCE)
    public void testDuplicateSubjectDn() throws Exception {
        var mgr1 = new DeviceCertificateManager(Mode.RSA, new X500Principal("OU=Tenant 1,OU=IoT,O=EnMasse,C=IO"));
        var mgr2 = new DeviceCertificateManager(Mode.RSA, new X500Principal("OU=Tenant 1,OU=IoT,O=EnMasse,C=IO"));

        final String name2 = UUID.randomUUID().toString();
        final String ns2 = UUID.randomUUID().toString();
        Kubernetes.getInstance().createNamespace(ns2);

        try (IoTTestSession session = IoTTestSession.createDefault()
                .project(project -> project.editOrNewSpec()
                        .editOrNewConfiguration()
                        .addNewTrustAnchor()
                        .withCertificate(toPem(mgr1.getCertificate()))
                        .endTrustAnchor()
                        .endConfiguration()
                        .endSpec())
                .adapters(HTTP)
                .deploy()) {

            // now create a second project, in second namespace, with a new CA
            // having the same name. This must fail.

            var project2 = new IoTProjectBuilder(IoTUtils.getBasicIoTProjectObject(
                    name2, name2, ns2,
                    AddressSpacePlans.STANDARD_SMALL))

                            .editOrNewSpec()
                            .editOrNewConfiguration()
                            .addNewTrustAnchor()
                            .withCertificate(toPem(mgr2.getCertificate()))
                            .endTrustAnchor()
                            .endConfiguration()
                            .endSpec()

                            .build();

            // create

            var projectClient = Kubernetes.getInstance().getIoTProjectClient(ns2);
            session.addCleanup(() -> IoTUtils.deleteIoTProjectAndWait(project2));
            projectClient.create(project2);
            var projectAccess = projectClient.withName(name2);

            // and wait for the condition

            TestUtils.waitUntilConditionOrFail(() -> {

                // wait until the condition 'TrustAnchorsUnique' becomes false
                return Optional
                        .ofNullable(projectAccess.get())
                        .map(IoTProject::getStatus)
                        .map(IoTProjectStatus::getConditions)
                        .flatMap(c -> c.stream()
                                .filter(condition -> condition.getType() == ProjectConditionType.TRUST_ANCHORS_UNIQUE)
                                .findAny())
                        .map(c -> "false".equalsIgnoreCase(c.getStatus()) &&
                                "DuplicateTrustAnchors".equals(c.getReason()))
                        .orElse(false);

            }, Duration.ofMinutes(5), Duration.ofSeconds(10), () -> "Conditions 'TrustAnchorsUnique' should become 'false'");

            log.info("Successfully detected 'DuplicateTrustAnchors' problem on second project");

        }

    }

}
