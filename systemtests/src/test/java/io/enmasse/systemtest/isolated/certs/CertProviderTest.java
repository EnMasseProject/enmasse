/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.certs;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.CertSpecBuilder;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.EndpointSpecBuilder;
import io.enmasse.address.model.ExposeType;
import io.enmasse.address.model.TlsTermination;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.apiclients.OpenshiftCertValidatorApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.certs.CertProvider;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.shared.standard.QueueTest;
import io.enmasse.systemtest.shared.standard.TopicTest;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.ISOLATED;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(ISOLATED)
class CertProviderTest extends TestBase {

    private static Logger log = CustomLogger.getLogger();

    private AddressSpace addressSpace;
    private UserCredentials user;
    private Address queue;
    private Address topic;

    @Test
    @OpenShift
    void testSelfSigned() throws Exception {

        CertSpec spec = new CertSpecBuilder()
                .withProvider(CertProvider.selfsigned.name())
                .build();

        createTestEnv(
                createEndpoint("messaging", spec, null, "amqps"));

        String caCert = new String(Base64.getDecoder().decode(resourceManager.getAddressSpace(addressSpace.getMetadata().getName()).getStatus().getCaCert()));

        testCertProvider(caCert);
    }

    @Test
    @OpenShift
    void testCertBundle() throws Exception {
        String domain = environment.kubernetesDomain();
        String messagingHost = String.format("messaging.%s", domain);
        CertBundle messagingCert = CertificateUtils.createCertBundle(messagingHost);

        createTestEnv(
                createEndpoint("messaging", new CertSpecBuilder()
                                .withProvider(CertProvider.certBundle.name())
                                .withTlsKey(messagingCert.getKeyB64())
                                .withTlsCert(messagingCert.getCertB64())
                                .build(),
                        messagingHost,
                        "amqps"));

        testCertProvider(messagingCert.getCaCert());
    }

    @Test
    @OpenShift
    void testOpenshiftCertProvider() throws Exception {
        createTestEnv(false,
                new EndpointSpecBuilder()
                        .withName("messaging")
                        .withService("messaging")
                        .editOrNewCert()
                        .withProvider(CertProvider.openshift.name())
                        .endCert()
                        .build());
        String appNamespace = "certificate-validator-ns";
        boolean testSucceeded = false;
        try {
            SystemtestsKubernetesApps.deployOpenshiftCertValidator(appNamespace, kubernetes);
            try (var client = new OpenshiftCertValidatorApiClient(SystemtestsKubernetesApps.getOpenshiftCertValidatorEndpoint(appNamespace, kubernetes))) {

                JsonObject request = new JsonObject();
                request.put("username", user.getUsername());
                request.put("password", user.getPassword());

                Endpoint messagingEndpoint = kubernetes.getEndpoint("messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), environment.namespace(), "amqps");
                request.put("messagingHost", messagingEndpoint.getHost());
                request.put("messagingPort", messagingEndpoint.getPort());

                TimeoutBudget timeout = new TimeoutBudget(5, TimeUnit.MINUTES);
                Exception lastException = null;
                int maxRetries = 10;
                int retry = 0;
                while (!timeout.timeoutExpired()) {
                    try {
                        log.info("Making request to openshift-cert-validator {}", request);
                        JsonObject response = client.test(request);
                        if (response.containsKey("error")) {
                            if (retry < maxRetries) {
                                retry++;
                            } else {
                                fail("Error testing openshift provider " + response.getString("error"));
                            }
                        } else {
                            testSucceeded = true;
                            return;
                        }
                    } catch (Exception e) {
                        lastException = e;
                    }
                    log.debug("next iteration, remaining time: {}", timeout.timeLeft());
                    Thread.sleep(5000);
                }
                log.error("Timeout expired");
                if (lastException != null) {
                    throw lastException;
                }
            }
        } finally {
            if (!testSucceeded) {
                logCollector.collectLogsOfPodsByLabels(appNamespace,
                        null, Collections.singletonMap("app", SystemtestsKubernetesApps.OPENSHIFT_CERT_VALIDATOR));
            }
            SystemtestsKubernetesApps.deleteOpenshiftCertValidator(appNamespace, kubernetes);
        }
    }

    private void createTestEnv(EndpointSpec... endpoints) throws Exception {
        createTestEnv(true, endpoints);
    }

    private void createTestEnv(boolean createAddresses, EndpointSpec... endpoints) throws Exception {
        addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-cert-provider-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .withEndpoints(endpoints)
                .endSpec()
                .build();

        resourceManager.createAddressSpace(addressSpace);

        user = new UserCredentials("user1", "password1");
        resourceManager.createOrUpdateUser(addressSpace, user);

        if (createAddresses) {
            queue = new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(addressSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("test-queue")
                    .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                    .endSpec()
                    .build();
            topic = new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(addressSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-topic"))
                    .endMetadata()
                    .withNewSpec()
                    .withType("topic")
                    .withAddress("test-topic")
                    .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                    .endSpec()
                    .build();
            resourceManager.setAddresses(queue, topic);
        }
    }

    private void testCertProvider(String messagingCert) throws Exception {
        AmqpClient amqpClient = resourceManager.getAmqpClientFactory().createQueueClient(addressSpace);
        amqpClient.getConnectOptions().setCredentials(user).setCert(messagingCert);

        QueueTest.runQueueTest(amqpClient, queue, 5);
        TopicTest.runTopicTest(amqpClient, topic, 5);
    }

    private static EndpointSpec createEndpoint(String name, CertSpec certSpec, String host, String servicePort) {
        EndpointSpecBuilder builder = new EndpointSpecBuilder()
                .withName(name)
                .withService(name)
                .withCert(certSpec)
                .withNewExpose()
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.passthrough)
                .withRouteServicePort(servicePort)
                .endExpose();
        if (host != null) {
            builder.editExpose().withRouteHost(host).endExpose();
        }
        return builder.build();
    }


}
