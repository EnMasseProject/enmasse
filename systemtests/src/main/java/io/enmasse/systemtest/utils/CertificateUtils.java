/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import io.enmasse.systemtest.CertBundle;
import io.enmasse.systemtest.executor.Executor;

public class CertificateUtils {

    //TODO refactor CertProviderTest to use this class

    private static void createSelfSignedCert(File cert, File key) throws Exception {
        new Executor().execute(Arrays.asList("openssl", "req", "-new", "-days", "11000", "-x509", "-batch", "-nodes",
                "-out", cert.getAbsolutePath(), "-keyout", key.getAbsolutePath()));
    }

    public static void createCsr(File keyFile, File csrFile) throws Exception {
        String subjString = "/O=enmasse-systemtests";
        new Executor().execute(Arrays.asList("openssl", "req", "-new", "-batch", "-nodes", "-keyout",
                keyFile.getAbsolutePath(), "-subj", subjString, "-out", csrFile.getAbsolutePath()));
    }

    public static File signCsr(File caKey, File caCert, File csrKey, File csrCsr) throws Exception {
        File crtFile = createTempFile(FilenameUtils.removeExtension(csrKey.getName()), "crt");
        new Executor().execute(Arrays.asList("openssl", "x509", "-req", "-days", "11000", "-in",
                csrCsr.getAbsolutePath(), "-CA", caCert.getAbsolutePath(), "-CAkey", caKey.getAbsolutePath(),
                "-CAcreateserial", "-out", crtFile.getAbsolutePath()));
        return crtFile;
    }

    private static File createTempFile(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        return file;
    }

    public static CertBundle createCertBundle() throws Exception {
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
