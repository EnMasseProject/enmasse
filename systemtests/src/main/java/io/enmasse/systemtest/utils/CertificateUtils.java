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

    public static void createCsr(File keyFile, File csrFile) throws Exception {
        new Executor().execute(Arrays.asList("openssl", "req", "-new", "-batch", "-nodes", "-keyout",
                keyFile.getAbsolutePath(), "-subj", SUBJECT, "-out", csrFile.getAbsolutePath()));
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
        createCsr(keyFile, csrFile);
        File crtFile = signCsr(caKey, caCert, keyFile, csrFile);
        try {
            return new CertBundle(FileUtils.readFileToString(caCert, StandardCharsets.UTF_8),
                    FileUtils.readFileToString(keyFile, StandardCharsets.UTF_8),
                    FileUtils.readFileToString(crtFile, StandardCharsets.UTF_8));
        } finally {
            deleteFiles(caCert, caKey, keyFile, csrFile, crtFile);
        }
    }

    public static BrokerCertBundle createBrokerCertBundle() throws Exception {
        File caCert = File.createTempFile("certAuthority", "crt");
        File caKey = File.createTempFile("certAuthority", "key");
        try {
            createSelfSignedCert(caCert, caKey);
            String broker = "broker";
            File brokerKey = File.createTempFile(broker, "key");
            File brokerCsr = File.createTempFile(broker, "csr");
            createCsr(brokerKey, brokerCsr);
            File brokerCrt = signCsr(caKey, caCert, brokerKey, brokerCsr);
//            CertBundle brokerBundle = new CertBundle(FileUtils.readFileToString(caCert, StandardCharsets.UTF_8),
//                        FileUtils.readFileToString(brokerKey, StandardCharsets.UTF_8),
//                        FileUtils.readFileToString(brokerCrt, StandardCharsets.UTF_8));
            //
            String client = UUID.randomUUID().toString();
            File clientKey = File.createTempFile(client, "key");
            File clientCsr = File.createTempFile(client, "csr");
            createCsr(clientKey, clientCsr);
            File clientCrt = signCsr(caKey, caCert, clientKey, clientCsr);
//            CertBundle clientBundle = new CertBundle(FileUtils.readFileToString(caCert, StandardCharsets.UTF_8),
//                        FileUtils.readFileToString(clientKey, StandardCharsets.UTF_8),
//                        FileUtils.readFileToString(clientCrt, StandardCharsets.UTF_8));
            //
            File brokerKeystore = new File("/tmp/"+UUID.randomUUID().toString());

            //create broker keystore
            //keytool -genkey -alias broker-keystore -keyalg RSA -keystore broker-test.ks --storepass 123456 -noprompt -dname "CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown"
            new Executor()
                .execute(Arrays.asList("keytool", "-genkey", "-alias", "broker-keystore",
                        "-keyalg", "RSA", "-keystore", brokerKeystore.getAbsolutePath(),
                        "--storepass", "123456", "-noprompt", "-dname", SUBJECT));//"OU=Unknown,L=Unknown,ST=Unknown,C=Unknown"));

            //import broker cert into keystore

            //keytool -import -trustcacerts -alias root -file ca.crt -keystore broker.ks -noprompt -storepass 123456
            new Executor()
                .execute(Arrays.asList("keytool", "-import", "-trustcacerts", "-alias", "root",
                        "-file", caCert.getAbsolutePath(), "-keystore", brokerKeystore.getAbsolutePath(), "-noprompt", "-storepass", "123456"));

            //keytool -import -trustcacerts -alias broker -file broker.crt -keystore broker.ks -noprompt -storepass 123456
            new Executor()
                .execute(Arrays.asList("keytool", "-import", "-trustcacerts", "-alias", "broker", "-file", brokerCrt.getAbsolutePath(), "-keystore", brokerKeystore.getAbsolutePath(), "-noprompt", "-storepass", "123456"));

            //import client cert into broker TRUSTSTORE
            File brokerTrustStore = new File("/tmp/"+UUID.randomUUID().toString());


            //keytool -import -alias client -keystore broker.ts -file client_cert
            new Executor()
                .execute(Arrays.asList("keytool", "-import", "-alias", "client", "-keystore",
                        brokerTrustStore.getAbsolutePath(), "-file", clientCrt.getAbsolutePath(), "-noprompt", "-storepass", "123456"));

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
