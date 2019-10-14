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
import io.enmasse.systemtest.VertxFactory;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.apiclients.OpenshiftCertValidatorApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.certs.CertProvider;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.shared.standard.QueueTest;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

class CertProviderTest extends TestBase implements ITestIsolatedStandard {

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
                createEndpoint("messaging", spec, null, "amqps"),
                createEndpoint("mqtt", spec, null, "secure-mqtt"));

        String caCert = new String(Base64.getDecoder().decode(resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getStatus().getCaCert()));

        testCertProvider(caCert, caCert);
    }

    @Test
    @OpenShift
    void testConsoleSelfSigned() throws Exception {
        CertSpec spec = new CertSpecBuilder()
                .withProvider(CertProvider.selfsigned.name())
                .build();

        createTestEnv(
                createEndpoint("console", spec, null, "https"));

        String endpointCert = new String(Base64.getDecoder().decode(
                resourcesManager.getAddressSpace(addressSpace.getMetadata().getName())
                        .getStatus()
                        .getEndpointStatuses()
                        .stream()
                        .filter(e -> e.getName().equals("console"))
                        .findFirst()
                        .get()
                        .getCert()));


        testConsole(endpointCert);

        String caCert = new String(Base64.getDecoder().decode(
                resourcesManager.getAddressSpace(addressSpace.getMetadata().getName())
                        .getStatus().getCaCert()));

        testConsole(caCert);
    }

    @Test
    @OpenShift
    void testCertBundle() throws Exception {
        String domain = environment.kubernetesDomain();
        String messagingHost = String.format("messaging.%s", domain);
        String mqttHost = String.format("mqtt.%s", domain);
        CertBundle messagingCert = CertificateUtils.createCertBundle(messagingHost);
        CertBundle mqttCert = CertificateUtils.createCertBundle(mqttHost);

        createTestEnv(
                createEndpoint("messaging", new CertSpecBuilder()
                                .withProvider(CertProvider.certBundle.name())
                                .withTlsKey(messagingCert.getKeyB64())
                                .withTlsCert(messagingCert.getCertB64())
                                .build(),
                        messagingHost,
                        "amqps"),
                createEndpoint("mqtt", new CertSpecBuilder()
                                .withProvider(CertProvider.certBundle.name())
                                .withTlsKey(mqttCert.getKeyB64())
                                .withTlsCert(mqttCert.getCertB64())
                                .build(),
                        mqttHost,
                        "secure-mqtt"));


        testCertProvider(messagingCert.getCaCert(), mqttCert.getCaCert());
    }

    @Test
    @OpenShift
    void testConsoleCertBundle() throws Exception {
        String domain = environment.kubernetesDomain();
        String consoleHost = String.format("space-console.%s", domain);
        CertBundle certBundle = CertificateUtils.createCertBundle(consoleHost);

        createTestEnv(false,
                createEndpoint("console", new CertSpecBuilder()
                        .withProvider(CertProvider.certBundle.name())
                        .withTlsKey(certBundle.getKeyB64())
                        .withTlsCert(certBundle.getCertB64())
                        .build(), consoleHost, "https"));

        testConsole(certBundle.getCaCert());
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
                        .build(),
                new EndpointSpecBuilder()
                        .withName("mqtt")
                        .withService("mqtt")
                        .editOrNewCert()
                        .withProvider(CertProvider.openshift.name())
                        .endCert()
                        .build(),
                new EndpointSpecBuilder()
                        .withName("console")
                        .withService("console")
                        .editOrNewCert()
                        .withProvider(CertProvider.openshift.name())
                        .endCert()
                        .build());
        String appNamespace = "certificate-validator-ns";
        boolean testSucceeded = false;
        try {
            SystemtestsKubernetesApps.deployOpenshiftCertValidator(appNamespace, kubernetes);
            try (var client = new OpenshiftCertValidatorApiClient(kubernetes, SystemtestsKubernetesApps.getOpenshiftCertValidatorEndpoint(appNamespace, kubernetes))) {

                JsonObject request = new JsonObject();
                request.put("username", user.getUsername());
                request.put("password", user.getPassword());

                Endpoint messagingEndpoint = kubernetes.getEndpoint("messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), environment.namespace(), "amqps");
                request.put("messagingHost", messagingEndpoint.getHost());
                request.put("messagingPort", messagingEndpoint.getPort());

                Endpoint mqttEndpoint = kubernetes.getEndpoint("mqtt-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), environment.namespace(), "secure-mqtt");
                request.put("mqttHost", mqttEndpoint.getHost());
                request.put("mqttPort", Integer.toString(mqttEndpoint.getPort()));

                Endpoint consoleEndpoint = kubernetes.getEndpoint("console-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), environment.namespace(), "https");
                request.put("consoleHost", consoleEndpoint.getHost());
                request.put("consolePort", consoleEndpoint.getPort());

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
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED_WITH_MQTT)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .withEndpoints(endpoints)
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(addressSpace);

        user = new UserCredentials("user1", "password1");
        resourcesManager.createOrUpdateUser(addressSpace, user);

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
            resourcesManager.setAddresses(queue, topic);
        }
    }

    private void testCertProvider(String messagingCert, String mqttCert) throws Exception {
        AmqpClient amqpClient = getAmqpClientFactory().createQueueClient(addressSpace);
        amqpClient.getConnectOptions().setCredentials(user).setCert(messagingCert);

        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setSocketFactory(getSocketFactory(new ByteArrayInputStream(mqttCert.getBytes())));
        mqttOptions.setUserName(user.getUsername());
        mqttOptions.setPassword(user.getPassword().toCharArray());
        IMqttClient mqttClient = getMqttClientFactory().build()
                .addressSpace(addressSpace)
                .mqttConnectionOptions(mqttOptions).create();

        QueueTest.runQueueTest(amqpClient, queue, 5);
        mqttClient.connect();
        simpleMQTTSendReceive(topic, mqttClient, 3);
        mqttClient.disconnect();
    }

    private void testConsole(String endpointCert) throws Exception {
        WebClient webClient = WebClient.create(VertxFactory.create(), new WebClientOptions()
                .setSsl(true)
                .setFollowRedirects(false)
                .setTrustAll(false)
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(endpointCert)))
                .setVerifyHost(true));

        Endpoint consoleEndpoint = getConsoleEndpoint(addressSpace);
        CompletableFuture<Optional<Throwable>> promise = new CompletableFuture<>();
        webClient.get(consoleEndpoint.getPort(), consoleEndpoint.getHost(), "").ssl(true).send(ar -> {
            log.info("get console " + ar.toString());
            if (ar.succeeded()) {
                promise.complete(Optional.empty());
            } else {
                log.info("Exception in get console", ar.cause());
                promise.complete(Optional.of(ar.cause()));
            }
        });

        Optional<Throwable> optError = promise.get();
        optError.ifPresent(Assertions::fail);
    }

    private SSLSocketFactory getSocketFactory(InputStream caCrtFile) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        X509Certificate caCert = (X509Certificate) cf.generateCertificate(caCrtFile);

        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(caKs);

        SSLContext context = getMqttClientFactory().tryGetSSLContext("TLSv1.2", "TLSv1.1", "TLS", "TLSv1");
        context.init(null, tmf.getTrustManagers(), new SecureRandom());

        return context.getSocketFactory();
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
