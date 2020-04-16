/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.certs.openssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OpenSSLUtil {
    private static final Logger log = LoggerFactory.getLogger(OpenSSLUtil.class);

    public static CertPair createSelfSignedCert(String subject) {
        File key = null;
        File cert = null;
        boolean success = false;
        try {
            key = File.createTempFile("tls", ".key");
            cert = File.createTempFile("tls", ".crt");
            List<String> cmd = Arrays.asList("openssl", "req", "-new", "-days", "11000", "-x509", "-batch", "-nodes",
                    "-out", cert.getAbsolutePath(),
                    "-keyout", key.getAbsolutePath(),
                    "-subj",
                    subject);

            runCommand(cmd.toArray(new String[] {}));

            success = true;
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
    public static CertSigningRequest createCsr(CertPair target) {
        File csr = null;
        boolean success = false;
        try {
            csr = File.createTempFile("server", ".csr");
            runCommand("openssl", "req", "-new", "-batch", "-nodes", "-keyout", target.getKey().getAbsolutePath(), "-subj", target.getSubject(), "-out", csr.getAbsolutePath());
            success = true;
            return new CertSigningRequest(csr,target.getKey());
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

    public static CertPair signCrs(CertSigningRequest request, Collection<String> sans, CertPair ca) {
        File crt = null;
        boolean success = false;
        try {
            crt = File.createTempFile("server", ".crt");
            if (sans.size() > 0) {
                String sansString = "subjectAltName=DNS:" + sans.stream().collect(Collectors.joining(",DNS:"));
                runCommand("bash",
                        "-c",
                        "openssl x509 -req -extfile <(printf \"" + sansString + "\") -days 11000 -in " + request.getCsrFile().getAbsolutePath() +
                                " -CA " + ca.getCert().getAbsolutePath() +
                                " -CAkey " + ca.getKey().getAbsolutePath() +
                                " -CAcreateserial -out " + crt.getAbsolutePath());
            } else {
                runCommand("openssl",
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
                        crt.getAbsolutePath());
            }
            success = true;
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
            String pems = runCommandWithInput("GET / HTTP/1.1\n", cmd.toArray(new String[] {}));
            Files.writeString(cert.toPath(), pems);
            success = true;
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

    private static String runCommand(String... cmd) {
        return runCommandWithInput(null, cmd);
    }

    private static String runCommandWithInput(String stdin, String... cmd) {
        ProcessBuilder keyGenBuilder = new ProcessBuilder(cmd).redirectInput(ProcessBuilder.Redirect.PIPE).redirectErrorStream(true);

        log.info("Running command '{}'", keyGenBuilder.command());
        String outBuf = null;
        boolean success = false;
        try {
            Process process = keyGenBuilder.start();
            try (Writer writer = new OutputStreamWriter(process.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                if (stdin != null) {
                    writer.write(stdin);
                }
                writer.close();

                outBuf = reader.lines().collect(Collectors.joining("\n"));
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
            if (!success && outBuf != null) {
                log.error("Command output: {}", outBuf);
            }
        }
        return outBuf.toString();
    }
}
