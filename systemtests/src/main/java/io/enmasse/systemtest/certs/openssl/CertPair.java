/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.certs.openssl;

import java.io.Closeable;
import java.io.File;

public class CertPair implements Closeable {
    private final File key;
    private final File cert;
    private String subject;

    public CertPair(File key, File cert, String subject) {
        this.key = key;
        this.cert = cert;
        this.subject = subject;
    }

    public File getKey() {
        return key;
    }

    public File getCert() {
        return cert;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public void close() {
        if (key != null) {
            key.delete();
        }
        if (cert != null) {
            cert.delete();
        }
    }
}
