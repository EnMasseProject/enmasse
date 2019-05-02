/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller that creates self-signed certificates for instances.
 */
public class OpenSSLCertManager implements CertManager {
    private static final Logger log = LoggerFactory.getLogger(OpenSSLCertManager.class);
    private static final int PROCESS_LINE_BUFFER_SIZE = 10;
    private final KubernetesClient client;
    private final String namespace;
    private final File certDir;

    public OpenSSLCertManager(KubernetesClient controllerClient,
                              File certDir) {
        this.client = controllerClient;
        this.certDir = certDir;
        this.namespace = controllerClient.getNamespace();
    }

    private static void createSelfSignedCert(final File keyFile, final File certFile) {
        runCommand("openssl", "req", "-new", "-days", "11000", "-x509", "-batch", "-nodes",
                "-out", certFile.getAbsolutePath(), "-keyout", keyFile.getAbsolutePath());
    }

    private Secret createSecretFromCertAndKeyFiles(final String secretName,
                                                        final Map<String, String> secretLabels,
                                                        final File keyFile,
                                                        final File certFile,
                                                        final KubernetesClient client)
            throws IOException {
        return createSecretFromCertAndKeyFiles(secretName, secretLabels, "tls.key", "tls.crt", keyFile, certFile, client);
    }

    private Secret createSecretFromCertAndKeyFiles(final String secretName,
                                                          final Map<String, String> secretLabels,
                                                          final String keyKey,
                                                          final String certKey,
                                                          final File keyFile,
                                                          final File certFile,
                                                          final KubernetesClient client)
            throws IOException {
        Map<String, String> data = new LinkedHashMap<>();
        Base64.Encoder encoder = Base64.getEncoder();
        data.put(keyKey, encoder.encodeToString(FileUtils.readFileToByteArray(keyFile)));
        data.put(certKey, encoder.encodeToString(FileUtils.readFileToByteArray(certFile)));
        return client.secrets().inNamespace(namespace).withName(secretName).createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(secretName)
                .withLabels(secretLabels)
                .endMetadata()
                .addToData(data)
                .done();
    }

    private static void runCommand(String... cmd) {
        ProcessBuilder keyGenBuilder = new ProcessBuilder(cmd).redirectErrorStream(true);

        log.info("Running command '{}'", keyGenBuilder.command());
        Deque<String> outBuf = new LinkedBlockingDeque<>(PROCESS_LINE_BUFFER_SIZE);
        boolean success = false;
        try {
            Process process = keyGenBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    boolean added = outBuf.offerLast(line);
                    if (log.isDebugEnabled()) {
                        log.debug("Command output: {}", line);
                    }
                    if (!added) {
                        outBuf.removeFirst();
                        outBuf.addLast(line);
                    }
                }
            }
            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                throw new RuntimeException(String.format("Command '%s' timed out", keyGenBuilder.command()));
            }

            final int exitValue = process.waitFor();
            success = exitValue == 0;
            String msg = String.format("Command '%s' completed with exit value %d", keyGenBuilder.command(), exitValue);
            if (success) {
                log.info(msg);
            } else {
                log.error(msg);
                throw new RuntimeException(String.format("Command '%s' failed with exit value %d", keyGenBuilder.command(), exitValue));
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!success && !outBuf.isEmpty()) {
                log.error("Last {} line(s) written by command to stdout/stderr follow", outBuf.size());
                outBuf.forEach(line -> log.error("Command output: {}", line));
            }
        }
    }

    @Override
    public Collection<CertComponent> listComponents(String uuid) {
        List<HasMetadata> components = new ArrayList<>();

        components.addAll(client.apps().deployments().inNamespace(namespace).withLabel(LabelKeys.INFRA_UUID, uuid).list().getItems());
        components.addAll(client.apps().statefulSets().inNamespace(namespace).withLabel(LabelKeys.INFRA_UUID, uuid).list().getItems());

        return components.stream()
                .filter(object -> object.getMetadata().getAnnotations() != null && object.getMetadata().getAnnotations().containsKey(AnnotationKeys.CERT_SECRET_NAME))
                .map(object -> {
                    Map<String, String> annotations = object.getMetadata().getAnnotations();
                    String cn = annotations.getOrDefault(AnnotationKeys.CERT_CN, object.getMetadata().getName());
                    return new CertComponent(cn, uuid, annotations.get(AnnotationKeys.CERT_SECRET_NAME));
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean certExists(CertComponent component) {
        return client.secrets().inNamespace(namespace).withName(component.getSecretName()).get() != null;
    }

    @Override
    public Secret getCertSecret(String name) {
        return client.secrets().inNamespace(namespace).withName(name).get();
    }


    @Override
    public CertSigningRequest createCsr(CertComponent component) {
        File keyFile = new File(certDir, component.getName() + "." + component.getUuid() + ".key");
        File csrFile = new File(certDir, component.getName() + "." + component.getUuid() + ".csr");
        String subjString = "/O=io.enmasse";
        if (component.getName().length() <= 64) {
            subjString += "/CN=" + component.getName();
        }
        runCommand("openssl", "req", "-new", "-batch", "-nodes", "-keyout", keyFile.getAbsolutePath(), "-subj", subjString, "-out", csrFile.getAbsolutePath());
        return new CertSigningRequest(component, csrFile, keyFile);
    }

    @Override
    public Cert signCsr(CertSigningRequest request, Secret secret, Collection<String> sans) {
        File crtFile = new File(certDir, request.getCertComponent().getName() + "." + request.getCertComponent().getUuid() + ".crt");

        File caKey = createTempFileFromSecret(secret, "tls.key");
        File caCert = createTempFileFromSecret(secret, "tls.crt");

        try {
            if (sans.size() > 0) {
                String sansString = "subjectAltName=DNS:" + sans.stream().collect(Collectors.joining(",DNS:"));
                runCommand("bash",
                        "-c",
                        "openssl x509 -req -extfile <(printf \"" + sansString + "\") -days 11000 -in " + request.getCsrFile().getAbsolutePath() +
                                " -CA " + caCert.getAbsolutePath() +
                                " -CAkey " + caKey.getAbsolutePath() +
                                " -CAcreateserial -out " + crtFile.getAbsolutePath());
            } else {
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
            }
            return new Cert(request.getCertComponent(), request.getKeyFile(), crtFile);
        } finally {
            caKey.delete();
            caCert.delete();
        }

    }

    private File createTempFileFromSecret(Secret secret, String key) {
        try {
            if (secret.getData() == null || secret.getData().get(key) == null) {
                throw new IllegalStateException(String.format("No secret data found for key '%s'", key));
            }
            String data = secret.getData().get(key);
            File file = File.createTempFile("secret", "pem");
            final Base64.Decoder decoder = Base64.getDecoder();
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(decoder.decode(data));
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Secret createSecret(Cert cert, Secret caSecret, Map<String, String> labels) {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            Base64.Encoder encoder = Base64.getEncoder();
            data.put("tls.key", encoder.encodeToString(FileUtils.readFileToByteArray(cert.getKeyFile())));
            data.put("tls.crt", encoder.encodeToString(FileUtils.readFileToByteArray(cert.getCertFile())));
            data.put("ca.crt", caSecret.getData().get("tls.crt"));

            return client.secrets().inNamespace(namespace).createNew()
                    .editOrNewMetadata()
                    .withName(cert.getComponent().getSecretName())
                    .withLabels(labels)
                    .endMetadata()
                    .withType("kubernetes.io/tls")
                    .addToData(data)
                    .done();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Secret createSelfSignedCertSecret(final String secretName, Map<String, String> labels) {
        try {
            File key = File.createTempFile("tls", "key");
            File cert = File.createTempFile("tls", "crt");
            try {
                createSelfSignedCert(key, cert);

                return createSecretFromCertAndKeyFiles(secretName, labels, key, cert, this.client);
            } finally {
                key.delete();
                cert.delete();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static OpenSSLCertManager create(KubernetesClient controllerClient) {
        return new OpenSSLCertManager(controllerClient, new File("/tmp"));
    }

}
