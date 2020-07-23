/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.certs.ssl;

import io.enmasse.systemtest.certs.BrokerCertBundle;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.executor.ExecutionResultData;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SSLUtils {
    public static final String DEFAULT_SUBJECT = "/O=enmasse-systemtests";

    public static CertPair createSelfSignedCert() {
        return createSelfSignedCert(DEFAULT_SUBJECT);
    }

    public static CertPair createSelfSignedCert(String subject) {
        File key = null;
        File cert = null;
        boolean success = false;
        try {
            key = File.createTempFile("tls", ".key");
            cert = File.createTempFile("tls", ".crt");

            success = Exec.executeAndCheck("openssl", "req", "-new", "-days", "11000", "-x509", "-batch", "-nodes",
                    "-out", cert.getAbsolutePath(),
                    "-keyout", key.getAbsolutePath(),
                    "-subj",
                    subject).getRetCode();

            return new CertPair(key, cert, subject);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!success) {

                if (key != null) {
                    key.delete();
                }
                if (cert != null) {
                    cert.delete();
                }
            }
        }
    }

    public static CertPair createStore(CertPair cert, String name) {
        File keystore = null;
        File p12Store = null;
        boolean success = false;
        try {
            keystore = File.createTempFile("tls", ".jks");
            p12Store = File.createTempFile("tls", ".p12");
            success = Exec.executeAndCheck("openssl", "pkcs12", "-export", "-passout", "pass:123456",
                    "-in", cert.getCert().getAbsolutePath(), "-inkey", cert.getKey().getAbsolutePath(), "-name", name, "-out", p12Store.getAbsolutePath()).getRetCode();

            keystore.delete();
            success = Exec.executeAndCheck("keytool", "-importkeystore", "-srcstorepass", "123456",
                    "-deststorepass", "123456", "-destkeystore", keystore.getAbsolutePath(), "-srckeystore", p12Store.getAbsolutePath(), "-srcstoretype", "PKCS12").getRetCode();

            return new CertPair(null, keystore, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!success) {

                if (keystore != null) {
                    keystore.delete();
                }
                if (p12Store != null) {
                    p12Store.delete();
                }
            }
        }
    }

    public static CertSigningRequest createCsr(CertPair target) {
        File csr = null;
        boolean success = false;
        try {
            csr = File.createTempFile("server", ".csr");
            success = Exec.executeAndCheck("openssl", "req", "-new", "-batch", "-nodes", "-keyout", target.getKey().getAbsolutePath(), "-subj", target.getSubject(), "-out", csr.getAbsolutePath()).getRetCode();
            return new CertSigningRequest(csr, target.getKey());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!success) {
                if (csr != null) {
                    csr.delete();
                }
            }
        }
    }

    public static CertPair signCsr(CertSigningRequest request, Collection<String> sans, CertPair ca) {
        File crt = null;
        boolean success = false;
        try {
            crt = File.createTempFile("server", ".crt");
            if (sans.size() > 0) {
                String sansString = "subjectAltName=DNS:" + String.join(",DNS:", sans);
                success = Exec.executeAndCheck("bash",
                        "-c",
                        "openssl x509 -req -extfile <(printf \"" + sansString + "\") -days 11000 -in " + request.getCsrFile().getAbsolutePath() +
                                " -CA " + ca.getCert().getAbsolutePath() +
                                " -CAkey " + ca.getKey().getAbsolutePath() +
                                " -CAcreateserial -out " + crt.getAbsolutePath()).getRetCode();
            } else {
                success = Exec.executeAndCheck("openssl",
                        "x509",
                        "-req",
                        "-days",
                        "11000",
                        "-in",
                        request.getCsrFile().getAbsolutePath(),
                        "-CA",
                        ca.getCert().getAbsolutePath(),
                        "-CAkey",
                        ca.getKey().getAbsolutePath(),
                        "-CAcreateserial",
                        "-out",
                        crt.getAbsolutePath()).getRetCode();
            }
            return new CertPair(request.getKeyFile(), crt, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!success) {
                if (crt != null) {
                    crt.delete();
                }
            }
        }
    }

    public static CertPair downloadCert(String host, int port) {
        File cert = null;
        boolean success = false;
        try {
            cert = File.createTempFile(String.format("host_%s:%d", host, port), ".crt");
            List<String> cmd = Arrays.asList("openssl", "s_client", "-crlf", "-showcerts", "-servername", host, "-connect", String.format("%s:%d", host, port));
            ExecutionResultData data = Exec.executeAndCheck("GET / HTTP/1.1\n", cmd);
            Files.writeString(cert.toPath(), data.getStdOut());
            success = data.getRetCode();
            return new CertPair(null, cert, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!success) {
                if (cert != null) {
                    cert.delete();
                }
            }
        }
    }

    public static CertBundle createCertBundle(String cn) throws Exception {
        CertPair ca = createSelfSignedCert();
        CertPair cert = createSelfSignedCert(DEFAULT_SUBJECT + "/CN=" + cn);
        CertSigningRequest csr = createCsr(cert);
        cert = signCsr(csr, Collections.emptyList(), ca);
        try {
            return new CertBundle(FileUtils.readFileToString(ca.getCert(), StandardCharsets.UTF_8),
                    FileUtils.readFileToString(cert.getKey(), StandardCharsets.UTF_8),
                    FileUtils.readFileToString(cert.getCert(), StandardCharsets.UTF_8));
        } finally {
            deleteFiles(ca.getCert(), ca.getKey(), cert.getKey(), cert.getCert());
        }
    }

    public static BrokerCertBundle createBrokerCertBundle(String cn) throws Exception {
        // Generate CA used to sign certs
        CertPair ca = createSelfSignedCert();
        try {
            // Create broker certs and put into keystore
            CertPair broker = createSelfSignedCert(DEFAULT_SUBJECT + "/CN=" + cn);
            CertSigningRequest brokerCsr = createCsr(broker);
            broker = signCsr(brokerCsr, Collections.emptyList(), ca);
            File brokerKeystore = createStore(broker, "broker").getCert();

            // Generate truststore with client cert and put into truststore
            CertPair client = createSelfSignedCert(DEFAULT_SUBJECT);
            CertSigningRequest clientCsr = createCsr(client);
            client = signCsr(clientCsr, Collections.emptyList(), ca);

            //import client cert into broker TRUSTSTORE
            File brokerTrustStore = createStore(client, "client").getCert();

            try {
                //return ca.crt keystore and truststore
                return new BrokerCertBundle(Files.readAllBytes(ca.getCert().toPath()),
                        Files.readAllBytes(brokerKeystore.toPath()),
                        Files.readAllBytes(brokerTrustStore.toPath()),
                        Files.readAllBytes(client.getCert().toPath()),
                        Files.readAllBytes(client.getKey().toPath()));
            } finally {
                deleteFiles(broker.getCert(), broker.getKey(), client.getCert(), client.getKey());
            }

        } finally {
            deleteFiles(ca.getCert(), ca.getKey());
        }
    }

    /**
     * Encode an X509 certificate into PEM format.
     *
     * @param certificates The certificates to encode.
     * @return the PEM encoded certificate, or {@code null} if the input was {@code null}.
     */
    public static String toPem(final X509Certificate... certificates) {

        if (certificates == null) {
            return null;
        }

        final StringWriter sw = new StringWriter();

        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            for (X509Certificate certificate : certificates) {
                pw.writeObject(certificate);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();

    }

    private static void deleteFiles(File... files) {
        for (File file : files) {
            FileUtils.deleteQuietly(file);
        }
    }
}
