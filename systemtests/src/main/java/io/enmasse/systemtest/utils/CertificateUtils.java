/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import io.enmasse.systemtest.certs.BrokerCertBundle;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.executor.Executor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;

public class CertificateUtils {

    private static final String SUBJECT = "/O=enmasse-systemtests";

    private static void createSelfSignedCert(File cert, File key) throws Exception {
        new Executor().execute(Arrays.asList("openssl", "req", "-new", "-days", "11000", "-x509", "-batch", "-nodes",
                "-out", cert.getAbsolutePath(), "-keyout", key.getAbsolutePath()));
    }

    public static void createCsr(File keyFile, File csrFile, String cn) throws Exception {
        String subject = SUBJECT;
        if (cn != null) {
            subject += "/CN=" + cn;
        }
        new Executor().execute(Arrays.asList("openssl", "req", "-new", "-batch", "-nodes", "-keyout",
                keyFile.getAbsolutePath(), "-subj", subject, "-out", csrFile.getAbsolutePath()));
    }

    public static File signCsr(File caKey, File caCert, File csrKey, File csrCsr) throws Exception {
        File crtFile = File.createTempFile(FilenameUtils.removeExtension(csrKey.getName()), "crt");
        new Executor().execute(Arrays.asList("openssl", "x509", "-req", "-days", "11000", "-in",
                csrCsr.getAbsolutePath(), "-CA", caCert.getAbsolutePath(), "-CAkey", caKey.getAbsolutePath(),
                "-CAcreateserial", "-out", crtFile.getAbsolutePath()));
        return crtFile;
    }

    public static CertBundle createCertBundle() throws Exception {
        File caCert = File.createTempFile("certAuthority", "crt");
        File caKey = File.createTempFile("certAuthority", "key");
        createSelfSignedCert(caCert, caKey);
        String randomName = UUID.randomUUID().toString();
        File keyFile = File.createTempFile(randomName, "key");
        File csrFile = File.createTempFile(randomName, "csr");
        createCsr(keyFile, csrFile, null);
        File crtFile = signCsr(caKey, caCert, keyFile, csrFile);
        try {
            return new CertBundle(FileUtils.readFileToString(caCert, StandardCharsets.UTF_8),
                    FileUtils.readFileToString(keyFile, StandardCharsets.UTF_8),
                    FileUtils.readFileToString(crtFile, StandardCharsets.UTF_8));
        } finally {
            deleteFiles(caCert, caKey, keyFile, csrFile, crtFile);
        }
    }

    public static BrokerCertBundle createBrokerCertBundle(String cn) throws Exception {
        File caCert = File.createTempFile("certAuthority", "crt");
        File caKey = File.createTempFile("certAuthority", "key");
        try {
            // Generate CA used to sign certs
            createSelfSignedCert(caCert, caKey);

            // Create broker certs and put into keystore
            String broker = "broker";
            File brokerKey = File.createTempFile(broker, "key");
            File brokerCsr = File.createTempFile(broker, "csr");
            createCsr(brokerKey, brokerCsr, cn);
            File brokerCrt = signCsr(caKey, caCert, brokerKey, brokerCsr);
            File brokerKeystore = File.createTempFile("broker-keystore", "jks");

            File p12Store = File.createTempFile("keystore", "p12");
            // create p12 keystore
            new Executor()
                    .execute(Arrays.asList("openssl", "pkcs12", "-export", "-passout", "pass:123456", "-in", brokerCrt.getAbsolutePath(), "-inkey", brokerKey.getAbsolutePath(), "-name", "broker", "-out", p12Store.getAbsolutePath()));

            brokerKeystore.delete();
            new Executor()
                    .execute(Arrays.asList("keytool", "-importkeystore", "-srcstorepass", "123456", "-deststorepass", "123456", "-destkeystore", brokerKeystore.getAbsolutePath(), "-srckeystore", p12Store.getAbsolutePath(), "-srcstoretype", "PKCS12"));

            // Generate truststore with client cert
            String client = UUID.randomUUID().toString();
            File clientKey = File.createTempFile(client, "key");
            File clientCsr = File.createTempFile(client, "csr");
            createCsr(clientKey, clientCsr, null);
            File clientCrt = signCsr(caKey, caCert, clientKey, clientCsr);
//            CertBundle clientBundle = new CertBundle(FileUtils.readFileToString(caCert, StandardCharsets.UTF_8),
//                        FileUtils.readFileToString(clientKey, StandardCharsets.UTF_8),
//                        FileUtils.readFileToString(clientCrt, StandardCharsets.UTF_8));

            //import client cert into broker TRUSTSTORE
            File brokerTrustStore = File.createTempFile("broker-truststore", "jks");

            //keytool -import -alias client -keystore broker.ts -file client_cert
            new Executor()
                .execute(Arrays.asList("keytool", "-import", "-alias", "client", "-keystore",
                        brokerTrustStore.getAbsolutePath(), "-file", caCert.getAbsolutePath(), "-noprompt", "-storepass", "123456"));

            try {
                //return ca.crt keystore and truststore
                return new BrokerCertBundle(FileUtils.readFileToString(caCert, StandardCharsets.UTF_8),
                        Files.readAllBytes(brokerKeystore.toPath()),
                        Files.readAllBytes(brokerTrustStore.toPath()));
            } finally {
                deleteFiles(brokerCrt, brokerCsr, brokerKey, clientCrt, clientCsr, clientKey);
            }

        } finally {
            deleteFiles(caCert, caKey);
        }
    }

    private static void deleteFiles(File... files) {
        for (File file : files) {
            FileUtils.deleteQuietly(file);
        }
    }
}
