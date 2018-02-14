/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.impl.Deployment;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller that creates self-signed certificates for instances.
 */
public class OpenSSLCertManager implements CertManager {
    private static final Logger log = LoggerFactory.getLogger(OpenSSLCertManager.class.getName());
    private final OpenShiftClient client;
    private final File certDir;
    private final String globalNamespace;

    public OpenSSLCertManager(OpenShiftClient controllerClient,
                              File certDir,
                              final String globalNamespace) {
        this.client = controllerClient;
        this.certDir = certDir;
        this.globalNamespace = globalNamespace;
    }

    @Override
    public void issueRouteCert(String secretName, String namespace, String ... hostnames) throws Exception {
        Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();
        if (secret == null) {
            String keyKey = "tls.key";
            String certKey = "tls.crt";

            log.info("Creating self-signed certificates for {}", secretName);
            File key = File.createTempFile("tls", "key");
            File cert = File.createTempFile("tls", "crt");

            createSelfSignedCert(key, cert);
            secret = createSecretFromCertAndKeyFiles(secretName, namespace, keyKey, certKey, key, cert, client);
        }

        ServiceAccount defaultAccount = client.serviceAccounts().inNamespace(namespace).withName("default").get();
        for (ObjectReference reference : defaultAccount.getSecrets()) {
            if (reference.getName().equals(secretName)) {
                return;
            }
        }

        client.serviceAccounts().inNamespace(namespace).withName("default").edit()
                .addToSecrets(new ObjectReferenceBuilder()
                        .withKind(secret.getKind())
                        .withName(secret.getMetadata().getName())
                        .withApiVersion(secret.getApiVersion())
                        .build())
                .done();
    }

    private static void createSelfSignedCert(final File keyFile, final File certFile) {
        runCommand("openssl", "req", "-new", "-days", "11000", "-x509", "-batch", "-nodes",
                "-out", certFile.getAbsolutePath(), "-keyout", keyFile.getAbsolutePath());
    }

    private static void createSecretFromCertAndKeyFiles(final String secretName,
                                                        final String namespace,
                                                        final File keyFile,
                                                        final File certFile,
                                                        final OpenShiftClient client)
            throws IOException {
        createSecretFromCertAndKeyFiles(secretName, namespace, "tls.key", "tls.crt", keyFile, certFile, client);
    }

    private static Secret createSecretFromCertAndKeyFiles(final String secretName,
                                                          final String namespace,
                                                          final String keyKey,
                                                          final String certKey,
                                                          final File keyFile,
                                                          final File certFile,
                                                          final OpenShiftClient client)
            throws IOException {
        Map<String, String> data = new LinkedHashMap<>();
        Base64.Encoder encoder = Base64.getEncoder();
        data.put(keyKey, encoder.encodeToString(FileUtils.readFileToByteArray(keyFile)));
        data.put(certKey, encoder.encodeToString(FileUtils.readFileToByteArray(certFile)));
        return client.secrets().inNamespace(namespace).withName(secretName).createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(secretName)
                .endMetadata()
                .addToData(data)
                .done();
    }

    private static void runCommand(String ... cmd) {
        ProcessBuilder keyGenBuilder = new ProcessBuilder(cmd).redirectErrorStream(true);

        log.info("Running command '{}'", keyGenBuilder.command());
        Process keyGen = null;
        try {
            keyGen = keyGenBuilder.start();
            InputStream stdout = keyGen.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            String line = null;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
            reader.close();
            if (!keyGen.waitFor(1, TimeUnit.MINUTES)) {
                throw new RuntimeException("Command timed out");
            }
        } catch (InterruptedException ignored) {
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Collection<CertComponent> listComponents(String namespace) {
        List<HasMetadata> components = new ArrayList<>();

        components.addAll(client.extensions().deployments().inNamespace(namespace).list().getItems());
        components.addAll(client.apps().statefulSets().inNamespace(namespace).list().getItems());

        return components.stream()
                .filter(object -> object.getMetadata().getAnnotations() != null && object.getMetadata().getAnnotations().containsKey(AnnotationKeys.CERT_SECRET_NAME))
                .map(object -> {
                    Map<String, String> annotations = object.getMetadata().getAnnotations();
                    String cn = annotations.getOrDefault(AnnotationKeys.CERT_CN, object.getMetadata().getName());
                    return new CertComponent(cn, namespace, annotations.get(AnnotationKeys.CERT_SECRET_NAME));
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean certExists(CertComponent component) {
        return client.secrets().inNamespace(component.getNamespace()).withName(component.getSecretName()).get() != null;
    }

    @Override
    public boolean certExists(String name) {
        return client.secrets().withName(name).get() != null;
    }


    @Override
    public CertSigningRequest createCsr(CertComponent component) {
        File keyFile = new File(certDir, component.getNamespace() + "." + component.getName() + ".key");
        File csrFile = new File(certDir, component.getNamespace() + "." + component.getName() + ".csr");
        runCommand("openssl", "req", "-new", "-batch", "-nodes", "-keyout", keyFile.getAbsolutePath(), "-subj", "/O=io.enmasse/CN=" + component.getName(), "-out", csrFile.getAbsolutePath());
        return new CertSigningRequest(component, csrFile, keyFile);
    }

    @Override
    public Cert signCsr(CertSigningRequest request, String secretName) {
        File crtFile = new File(certDir, request.getCertComponent().getNamespace() + "." + request.getCertComponent().getName() + ".crt");
        final Secret secret = client.secrets().withName(secretName).get();

        File caKey = createTempFileFromSecret(secret, "tls.key");
        File caCert = createTempFileFromSecret(secret, "tls.crt");

        try {
            runCommand("openssl",
                       "x509",
                       "-req",
                       "-days",
                       "11000",
                       "-in",
                       request.getCsrFile().getAbsolutePath(),
                       "-CA",
                       caCert.getAbsolutePath(),
                       "-CAkey",
                       caKey.getAbsolutePath(),
                       "-CAcreateserial",
                       "-out",
                       crtFile.getAbsolutePath());
            return new Cert(request.getCertComponent(), request.getKeyFile(), crtFile);
        } finally {
            caKey.delete();
            caCert.delete();
        }

    }

    private File createTempFileFromSecret(Secret secret, String entry) {
        try {
            File file = File.createTempFile("secret", "pem");
            String data = secret.getData().get(entry);
            final Base64.Decoder decoder = Base64.getDecoder();
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(decoder.decode(data));
            outputStream.close();
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void createSecret(Cert cert, final String caSecretName) {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            Base64.Encoder encoder = Base64.getEncoder();
            data.put("tls.key", encoder.encodeToString(FileUtils.readFileToByteArray(cert.getKeyFile())));
            data.put("tls.crt", encoder.encodeToString(FileUtils.readFileToByteArray(cert.getCertFile())));
            Secret caSecret = client.secrets().inNamespace(this.globalNamespace).withName(caSecretName).get();
            data.put("ca.crt", caSecret.getData().get("tls.crt"));

            client.secrets().inNamespace(cert.getComponent().getNamespace()).createNew()
                    .editOrNewMetadata()
                    .withName(cert.getComponent().getSecretName())
                    .endMetadata()
                    .withType("kubernetes.io/tls")
                    .addToData(data)
                    .done();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void createSelfSignedCertSecret(final String secretName) {
        try {
            File key = File.createTempFile("tls", "key");
            File cert = File.createTempFile("tls", "crt");
            try {
                createSelfSignedCert(key, cert);
                createSecretFromCertAndKeyFiles(secretName, this.globalNamespace, key, cert, this.client);
            } finally {
                key.delete();
                cert.delete();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static OpenSSLCertManager create(OpenShiftClient controllerClient,
                                            final String globalNamespace) {
        return new OpenSSLCertManager(controllerClient, new File("/tmp"), globalNamespace);
    }

}
