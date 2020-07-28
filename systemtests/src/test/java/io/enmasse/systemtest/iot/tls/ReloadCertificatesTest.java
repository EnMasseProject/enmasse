/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.tls;

import io.enmasse.iot.model.v1.ConfigConditionType;
import io.enmasse.systemtest.framework.condition.OpenShift;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.IoTTestSession.TenantInstance.Device;
import io.enmasse.systemtest.iot.IoTTests;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.iot.Names;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.enmasse.systemtest.framework.condition.OpenShiftVersion.OCP4;
import static io.enmasse.systemtest.framework.TestTag.IOT;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.HTTP;
import static io.enmasse.systemtest.platform.Kubernetes.getClient;
import static io.enmasse.systemtest.time.TimeoutBudget.ofDuration;
import static io.enmasse.systemtest.utils.Conditions.condition;
import static io.enmasse.systemtest.utils.TestUtils.waitForChangedResourceVersion;
import static io.enmasse.systemtest.utils.TestUtils.waitUntilConditionOrFail;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(IOT)
public class ReloadCertificatesTest implements IoTTests {

    private static final String NAMESPACE = Kubernetes.getInstance().getInfraNamespace();

    @Test
    @OpenShift(version = OCP4)
    public void testRecreateCertificate() throws Exception {

        IoTTestSession
                .createDefault()
                .adapters(HTTP)
                .config(config -> {
                    // ensure we are using the service-ca
                    config.editOrNewSpec()
                        .withNewInterServiceCertificates()
                        .withNewServiceCAStrategy()
                        .endServiceCAStrategy()
                        .endInterServiceCertificates()

                        .editOrNewAdapters()
                        .editOrNewHttp()
                        // reset to use service CA endpoint secret
                        .withNewEndpoint().endEndpoint()
                        .endHttp()
                        .endAdapters()

                        .endSpec();
                })
                .run((session) -> {

                    var deviceId = Names.randomDevice();
                    var authId = UUID.randomUUID().toString();
                    var password = UUID.randomUUID().toString();
                    var device = session.newDevice(deviceId)
                            .register()
                            .setPassword(authId, password);

                    // ensure everything works before starting

                    assertTelemetryWorks(session, device);

                    // get current pod

                    var deploymentAccess = getClient()
                            .apps().deployments()
                            .inNamespace(NAMESPACE)
                            .withName("iot-http-adapter");

                    var pod = getClient()
                            .pods()
                            .inNamespace(NAMESPACE).withLabels(Map.of(
                                    "app", "enmasse",
                                    "name", "iot-http-adapter"))
                            .list().getItems().stream()
                            .map(p -> p.getMetadata().getName())
                            .findFirst()
                            .orElse(null);

                    assertNotNull(pod);

                    // then: reset http adapter key/cert

                    var deleteResult = getClient().secrets()
                            .inNamespace(NAMESPACE)
                            .withName("iot-http-adapter-tls")
                            .delete();

                    assertEquals(Boolean.TRUE, deleteResult);

                    final TimeoutBudget budget = ofDuration(ofMinutes(10));

                    // wait until the deployment has changed

                    var initialVersion = deploymentAccess.get().getMetadata().getResourceVersion();
                    waitForChangedResourceVersion(
                            budget.remaining(),
                            initialVersion,
                            () -> ofNullable(deploymentAccess.get())
                                    .map(d -> d.getMetadata().getResourceVersion())
                                    .orElse(initialVersion));

                    // and wait until the IoTConfig is ready again

                    waitUntilConditionOrFail(
                            condition(session.config(), ConfigConditionType.READY),
                            budget.remaining(),
                            ofSeconds(5)
                    );

                    // now try to send messages again

                    assertTelemetryWorks(session, device);

                });

    }

    protected void assertTelemetryWorks(final IoTTestSession session, final Device device) throws Exception {
        try (var adapterClient = device.createHttpAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofMillis(500))
                    .consumerFactory(ConsumerFactory.of(session.getConsumerClient(), session.getTenantId()))
                    .sender(adapterClient::send)
                    .amount(30)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();
        }

    }
}
