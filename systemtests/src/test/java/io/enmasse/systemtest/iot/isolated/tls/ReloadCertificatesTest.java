/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.tls;

import static io.enmasse.systemtest.TestTag.ISOLATED;
import static io.enmasse.systemtest.condition.OpenShiftVersion.OCP4;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.HTTP;
import static io.enmasse.systemtest.time.TimeoutBudget.ofDuration;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTIsolated;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.IoTTestSession.Device;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.client.KubernetesClient;

@Tag(ISOLATED)
public class ReloadCertificatesTest extends TestBase implements ITestIoTIsolated {

    private static final String NAMESPACE = Kubernetes.getInstance().getInfraNamespace();

    private KubernetesClient client = Kubernetes.getInstance().getClient();

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

                    var deviceId = UUID.randomUUID().toString();
                    var authId = UUID.randomUUID().toString();
                    var password = UUID.randomUUID().toString();
                    var device = session.newDevice(deviceId)
                            .register()
                            .setPassword(authId, password);

                    // ensure everything works before starting

                    assertTelemetryWorks(session, device);

                    // get current pod

                    var deploymentAccess = this.client
                            .apps().deployments()
                            .inNamespace(NAMESPACE)
                            .withName("iot-http-adapter");

                    var pod = this.client
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

                    var deleteResult = this.client.secrets()
                            .inNamespace(NAMESPACE)
                            .withName("iot-http-adapter-tls")
                            .delete();

                    assertEquals(Boolean.TRUE, deleteResult);

                    final TimeoutBudget budget = ofDuration(ofMinutes(10));

                    // wait until the deployment has changed

                    var initialVersion = deploymentAccess.get().getMetadata().getResourceVersion();
                    TestUtils.waitForChangedResourceVersion(budget, initialVersion, () -> {
                        return Optional.ofNullable(deploymentAccess.get())
                                .map(d -> d.getMetadata().getResourceVersion())
                                .orElse(initialVersion);
                    });

                    // and wait until the IoTConfig is ready again

                    IoTUtils.waitForIoTConfigReady(Kubernetes.getInstance(), session.getConfig());

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
