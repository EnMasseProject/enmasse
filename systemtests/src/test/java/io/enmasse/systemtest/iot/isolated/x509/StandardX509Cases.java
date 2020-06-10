/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.x509;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bouncycastle.asn1.x500.X500Name;

import io.enmasse.systemtest.iot.DeviceCertificateManager;
import io.enmasse.systemtest.iot.DeviceCertificateManager.Mode;
import io.enmasse.systemtest.iot.DeviceSupplier;
import io.enmasse.systemtest.iot.IoTTestSession.Device;
import io.enmasse.systemtest.iot.StandardIoTTests;

public interface StandardX509Cases extends StandardIoTTests {

    DeviceCertificateManager getCertificateManager();

    @Override
    default List<DeviceSupplier> getDevices() {

        return Arrays.asList(
                () -> getSession().newDevice()
                        .named("default")
                        .register()
                        .enableX509(getCertificateManager().createDevice()));

    }

    @Override
    default List<DeviceSupplier> getInvalidDevices() {

        return Arrays.asList(
                () -> notOurCaDevice(),
                () -> expiredCertificate(),
                () -> futureCertificate());

    }

    /**
     * Create a device which has a certificate signed by a CA with the same name, but which actually is
     * not the same CA.
     */
    default Device notOurCaDevice() throws Exception {

        var otherMgr = new DeviceCertificateManager(Mode.RSA, new X500Name("OU=Tenant 1,OU=IoT,O=EnMasse,C=IO"));

        return getSession().newDevice()
                .named("notOurCa")
                .register()
                .enableX509(otherMgr.createDevice());

    }

    /**
     * Create a device which has an expired certificate.
     */
    default Device expiredCertificate() throws Exception {

        final Instant now = Instant.now();

        return getSession().newDevice()
                .named("expiredCertificate")
                .register()
                .enableX509(getCertificateManager()
                        .createDevice(
                                UUID.randomUUID().toString(),
                                now.minus(Duration.ofDays(30)),
                                now));

    }

    /**
     * Create a device which has an valid date in the future.
     */
    default Device futureCertificate() throws Exception {

        final Instant now = Instant.now();

        return getSession().newDevice()
                .named("futureCertificate")
                .register()
                .enableX509(getCertificateManager()
                        .createDevice(
                                UUID.randomUUID().toString(),
                                now.plus(Duration.ofDays(100)),
                                now.plus(Duration.ofDays(200))));

    }

}
