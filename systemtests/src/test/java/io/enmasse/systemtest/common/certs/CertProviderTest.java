/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.certs;

import io.enmasse.address.model.*;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.apiclients.OpenshiftCertValidatorApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.common.api.ApiServerTest;
import io.enmasse.systemtest.standard.QueueTest;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(isolated)
class CertProviderTest extends TestBase {

    private static Logger log = CustomLogger.getLogger();
    private static String ENDPOINT_PREFIX = "test-endpoint-";

    private AddressSpace addressSpace;
    private UserCredentials user;
    private Address queue;
    private Address topic;

    @Test
    void testSelfSigned() throws Exception {

        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.selfsigned.name())
                .build());

        String caCert = new String(Base64.getDecoder().decode(getAddressSpace(addressSpace.getMetadata().getName()).getStatus().getCaCert()));

        AmqpClient amqpClient = amqpClientFactory.createQueueClient(addressSpace);
        amqpClient.getConnectOptions().setCredentials(user).setCert(caCert);

        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setSocketFactory(getSocketFactory(new ByteArrayInputStream(caCert.getBytes())));
        mqttOptions.setUserName(user.getUsername());
        mqttOptions.setPassword(user.getPassword().toCharArray());
        IMqttClient mqttClient = mqttClientFactory.build()
                .addressSpace(addressSpace)
                .mqttConnectionOptions(mqttOptions).create();

        testCertProvider(amqpClient, mqttClient);
    }

    @Test
    @Disabled("Disabled due to #2427")
    void testConsoleSelfSignedTrustEndpointCert() throws Exception {
        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.selfsigned.name())
                .build(), false);

        String caCert = new String(Base64.getDecoder().decode(getAddressSpace(addressSpace.getMetadata().getName()).getStatus().getCaCert()));

        WebClient webClient = WebClient.create(VertxFactory.create(), new WebClientOptions()
                .setSsl(true)
                .setTrustAll(false)
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(caCert)))
                .setVerifyHost(false));

        testConsole(webClient);
    }

    @Test
    @Disabled("Disabled due to #2427")
    void testConsoleSelfSignedTrustCaCert() throws Exception {
        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.selfsigned.name())
                .build(), false);

        String endpointCert = new String(Base64.getDecoder().decode(
                getAddressSpace(addressSpace.getMetadata().getName())
                        .getSpec()
                        .getEndpoints()
                        .stream()
                        .filter(e -> e.getService().equals("console"))
                        .findFirst()
                        .get()
                        .getCert()
                        .getTlsCert()));

        WebClient webClient = WebClient.create(VertxFactory.create(), new WebClientOptions()
                .setSsl(true)
                .setTrustAll(false)
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(endpointCert)))
                .setVerifyHost(false));

        testConsole(webClient);
    }

    @Test
    void testCertBundle() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();

        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.certBundle.name())
                .withTlsKey(certBundle.getKeyB64())
                .withTlsCert(certBundle.getCertB64())
                .build());

        AmqpClient amqpClient = amqpClientFactory.createQueueClient(addressSpace);
        amqpClient.getConnectOptions()
                .setCredentials(user)
                .getProtonClientOptions()
                .setSsl(true)
                .setHostnameVerificationAlgorithm("")
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(certBundle.getCaCert())))
                .setTrustAll(false);

        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setSocketFactory(getSocketFactory(new ByteArrayInputStream(certBundle.getCaCert().getBytes())));
        mqttOptions.setUserName(user.getUsername());
        mqttOptions.setPassword(user.getPassword().toCharArray());
        IMqttClient mqttClient = mqttClientFactory.build()
                .addressSpace(addressSpace)
                .mqttConnectionOptions(mqttOptions).create();

        testCertProvider(amqpClient, mqttClient);

    }

    @Test
    @Disabled("Disabled due to #2427")
    void testConsoleCertBundleTrustEndpointCert() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();

        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.certBundle.name())
                .withTlsKey(certBundle.getKeyB64())
                .withTlsCert(certBundle.getCertB64())
                .build(), false);

        WebClient webClient = WebClient.create(VertxFactory.create(), new WebClientOptions()
                .setSsl(true)
                .setTrustAll(false)
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(certBundle.getCert())))
                .setVerifyHost(false));

        testConsole(webClient);
    }

    @Test
    @Disabled("Disabled due to #2427")
    void testConsoleCertBundleTrustCaCert() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();

        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.certBundle.name())
                .withTlsKey(certBundle.getKeyB64())
                .withTlsCert(certBundle.getCertB64())
                .build(), false);

        WebClient webClient = WebClient.create(VertxFactory.create(), new WebClientOptions()
                .setSsl(true)
                .setTrustAll(false)
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(certBundle.getCaCert())))
                .setVerifyHost(false));

        testConsole(webClient);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = Environment.USE_MINUKUBE_ENV, matches = "true")
    void testOpenshiftCertProvider() throws Exception {
        createTestEnv(new CertSpecBuilder()
                        .withProvider(CertProvider.openshift.name())
                        .build(),
                false);
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
                while (!timeout.timeoutExpired()) {
                    try {
                        log.info("Making request to openshift-cert-validator {}", request);
                        JsonObject response = client.test(request);
                        if (response.containsKey("error")) {
                            fail("Error testing openshift provider " + response.getString("error"));
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

    private void createTestEnv(CertSpec endpointCert) throws Exception {
        createTestEnv(endpointCert, true);
    }

    private void createTestEnv(CertSpec endpointCert, boolean createAddresses) throws Exception {
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
                .withEndpoints(Arrays.asList(
                        createEndpointSpec("messaging", "amqps", endpointCert),
                        createEndpointSpec("console", "https", endpointCert),
                        createEndpointSpec("mqtt", "secure-mqtt", endpointCert)
                ))
                .endSpec()
                .build();

        createAddressSpace(addressSpace);

        user = new UserCredentials("user1", "password1");
        createOrUpdateUser(addressSpace, user);

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
            setAddresses(queue, topic);
        }
    }

    private EndpointSpec createEndpointSpec(String service, String servicePort, CertSpec endpointCert) {
        return new EndpointSpecBuilder()
                .withName(ENDPOINT_PREFIX + service)
                .withService(service)
                .withExpose(
                        new ExposeSpecBuilder()
                                .withRouteServicePort(servicePort)
                                .withType(ExposeType.route)
                                .withRouteTlsTermination(TlsTermination.passthrough)
                                .build())
                .withCert(endpointCert)
                .build();
    }

    private void testCertProvider(AmqpClient amqpClient, IMqttClient mqttClient) throws Exception {
        QueueTest.runQueueTest(amqpClient, queue);
        mqttClient.connect();
        ApiServerTest.simpleMQTTSendReceive(topic, mqttClient, 3);
        mqttClient.disconnect();
    }

    private void testConsole(WebClient webClient) throws Exception {
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

        SSLContext context = mqttClientFactory.tryGetSSLContext("TLSv1.2", "TLSv1.1", "TLS", "TLSv1");
        context.init(null, tmf.getTrustManagers(), new SecureRandom());

        return context.getSocketFactory();
    }


}
