/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.certs;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.slf4j.Logger;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.CertSpecBuilder;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.ExposeSpecBuilder;
import io.enmasse.address.model.ExposeType;
import io.enmasse.address.model.TlsTermination;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.SystemtestsKubernetesApps;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.VertxFactory;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.apiclients.OcpEnmasseAppApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.common.api.ApiServerTest;
import io.enmasse.systemtest.executor.Executor;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Tag(isolated)
class CertProviderTest extends TestBase {

    public enum CertProvider {
        certBundle,
        selfsigned,
        openshift,
        wildcard
    }

    private static Logger log = CustomLogger.getLogger();
    private static String ENDPOINT_PREFIX = "test-endpoint-";

    private List<File> createdFiles;

    private AddressSpace addressSpace;
    private UserCredentials user;
    private Address queue;
    private Address topic;

    @BeforeEach
    void setUp() {
        createdFiles = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        createdFiles.forEach(FileUtils::deleteQuietly);
    }

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
    void testConsoleSelfSignedTrustCaCert() throws Exception {
        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.selfsigned.name())
                .build(), false);

        String endpointCert = new String(Base64.getDecoder().decode(
                getAddressSpace(addressSpace.getMetadata().getName())
                        .getSpec()
                        .getEndpoints()
                        .stream()
                        .filter(e->e.getService().equals("console"))
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
        CertBundle certBundle = createCertBundle();

        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.certBundle.name())
                .withTlsKey(certBundle.getKey())
                .withTlsCert(certBundle.getCert())
                .build());

        AmqpClient amqpClient = amqpClientFactory.createQueueClient(addressSpace);
        amqpClient.getConnectOptions()
            .setCredentials(user)
            .getProtonClientOptions()
                .setSsl(true)
                .setHostnameVerificationAlgorithm("")
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertPath(certBundle.getCaCert().getAbsolutePath()))
                .setTrustAll(false);

        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setSocketFactory(getSocketFactory(new FileInputStream(certBundle.getCaCert())));
        mqttOptions.setUserName(user.getUsername());
        mqttOptions.setPassword(user.getPassword().toCharArray());
        IMqttClient mqttClient = mqttClientFactory.build()
                .addressSpace(addressSpace)
                .mqttConnectionOptions(mqttOptions).create();

        testCertProvider(amqpClient, mqttClient);

    }

    @Test
    void testConsoleCertBundleTrustEndpointCert() throws Exception {
        CertBundle certBundle = createCertBundle();

        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.certBundle.name())
                .withTlsKey(certBundle.getKey())
                .withTlsCert(certBundle.getCert())
                .build(), false);

        WebClient webClient = WebClient.create(VertxFactory.create(), new WebClientOptions()
                .setSsl(true)
                .setTrustAll(false)
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertPath(certBundle.getCrtFile().getAbsolutePath()))
                .setVerifyHost(false));

        testConsole(webClient);
    }

    @Test
    void testConsoleCertBundleTrustCaCert() throws Exception {
        CertBundle certBundle = createCertBundle();

        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.certBundle.name())
                .withTlsKey(certBundle.getKey())
                .withTlsCert(certBundle.getCert())
                .build(), false);

        WebClient webClient = WebClient.create(VertxFactory.create(), new WebClientOptions()
                .setSsl(true)
                .setTrustAll(false)
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertPath(certBundle.getCaCert().getAbsolutePath()))
                .setVerifyHost(false));

        testConsole(webClient);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = Environment.useMinikubeEnv, matches = "true")
    void testOpenshiftCertProvider() throws Exception {
        createTestEnv(new CertSpecBuilder()
                .withProvider(CertProvider.openshift.name())
                .build(),
                false);
        try {
            SystemtestsKubernetesApps.deployOcpEnmasseApp(environment.namespace(), kubernetes);
            OcpEnmasseAppApiClient client = new OcpEnmasseAppApiClient(kubernetes, SystemtestsKubernetesApps.getOcpEnmasseAppEndpoint(environment.namespace(), kubernetes));

            JsonObject request = new JsonObject();
            request.put("username", user.getUsername());
            request.put("password", user.getPassword());

            Endpoint messagingEndpoint = kubernetes.getEndpoint("messaging-"+AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), environment.namespace(), "amqps");
            request.put("messagingHost", messagingEndpoint.getHost());
            request.put("messagingPort", messagingEndpoint.getPort());

            Endpoint mqttEndpoint = kubernetes.getEndpoint("mqtt-"+AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), environment.namespace(), "secure-mqtt");
            request.put("mqttHost", mqttEndpoint.getHost());
            request.put("mqttPort", Integer.toString(mqttEndpoint.getPort()));

            Endpoint consoleEndpoint = kubernetes.getEndpoint("console-"+AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), environment.namespace(), "https");
            request.put("consoleHost", consoleEndpoint.getHost());
            request.put("consolePort", consoleEndpoint.getPort());

            log.info("Making request to openshift app {}", request);

            JsonObject response = client.test(request);
            if(response.containsKey("error")) {
                fail("Error testing openshift provider "+response.getString("error"));
            }
        }finally {
            logCollector.collectLogsOfPodsByLabels(Collections.singletonMap("app", SystemtestsKubernetesApps.OCP_ENMASSE_APP));
            SystemtestsKubernetesApps.deleteOcpEnmasseApp(environment.namespace(), kubernetes);
        }
    }

    private void createTestEnv(CertSpec endpointCert) throws Exception {
        createTestEnv(endpointCert, true);
    }

    private void createTestEnv(CertSpec endpointCert, boolean createAddresses) throws Exception {
        addressSpace = AddressSpaceUtils.createAddressSpaceObject("cert-provider-addr-space", AddressSpaceType.STANDARD, AuthenticationServiceType.STANDARD);
        addressSpace.getSpec().setEndpoints(Arrays.asList(
                createEndpointSpec("messaging", "amqps", endpointCert),
                createEndpointSpec("console", "https", endpointCert),
                createEndpointSpec("mqtt", "secure-mqtt", endpointCert)));
        createAddressSpace(addressSpace);

        user = new UserCredentials("user1", "password1");
        createUser(addressSpace, user);

        if(createAddresses) {
            queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.STANDARD_SMALL_QUEUE);
            topic = AddressUtils.createTopicAddressObject("mytopic", DestinationPlan.STANDARD_LARGE_TOPIC);
            setAddresses(addressSpace, queue, topic);
        }
    }

    private EndpointSpec createEndpointSpec(String service, String servicePort, CertSpec endpointCert) {
        return new EndpointSpec(ENDPOINT_PREFIX + service,
                service,
                new ExposeSpecBuilder()
                .withRouteServicePort(servicePort)
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.passthrough)
                .build(),
                endpointCert);
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
        webClient.get(consoleEndpoint.getPort(), consoleEndpoint.getHost(), "").ssl(true)
        .send(ar->{
            log.info("get console "+ar.toString());
            if (ar.succeeded()) {
                promise.complete(Optional.empty());
            } else {
                log.info("Exception in get console", ar.cause());
                promise.complete(Optional.of(ar.cause()));
            }
        });

        Optional<Throwable> optError = promise.get();
        if(optError.isPresent()) {
            fail(optError.get());
        }
    }

    private void createSelfSignedCert(File cert, File key) throws Exception {
        new Executor().execute(Arrays.asList("openssl", "req", "-new", "-days", "11000", "-x509", "-batch", "-nodes",
                "-out", cert.getAbsolutePath(), "-keyout", key.getAbsolutePath()));
    }

    public void createCsr(File keyFile, File csrFile) throws Exception {
        String subjString = "/O=enmasse-systemtests";
        new Executor().execute(Arrays.asList("openssl", "req", "-new", "-batch", "-nodes", "-keyout",
                keyFile.getAbsolutePath(), "-subj", subjString, "-out", csrFile.getAbsolutePath()));
    }

    public File signCsr(File caKey, File caCert, File csrKey, File csrCsr) throws Exception {
        File crtFile = createTempFile(FilenameUtils.removeExtension(csrKey.getName()), "crt");
        new Executor().execute(Arrays.asList("openssl", "x509", "-req", "-days", "11000", "-in",
                csrCsr.getAbsolutePath(), "-CA", caCert.getAbsolutePath(), "-CAkey", caKey.getAbsolutePath(),
                "-CAcreateserial", "-out", crtFile.getAbsolutePath()));
        return crtFile;
    }

    private File createTempFile(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        createdFiles.add(file);
        return file;
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

    private CertBundle createCertBundle() throws Exception {
        File caCert = createTempFile("certAuthority", "crt");
        File caKey = createTempFile("certAuthority", "key");
        createSelfSignedCert(caCert, caKey);
        String randomName = UUID.randomUUID().toString();
        File keyFile = createTempFile(randomName, "key");
        File csrFile = createTempFile(randomName, "csr");
        createCsr(keyFile, csrFile);
        File crtFile = signCsr(caKey, caCert, keyFile, csrFile);
        String key = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(keyFile));
        String cert = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(crtFile));
        return new CertBundle(caCert, crtFile, key, cert);
    }

}
