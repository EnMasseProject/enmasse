/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.certs.openssl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class CertSigningRequest implements Closeable {
    private final File csrFile;
    private final File keyFile;

    CertSigningRequest(File csrFile, File keyFile) {
        this.csrFile = csrFile;
        this.keyFile = keyFile;
    }

    public File getCsrFile() {
        return csrFile;
    }

    public File getKeyFile() {
        return keyFile;
    }

    @Override
    public void close() throws IOException {
        csrFile.delete();
        keyFile.delete();
    }
}
