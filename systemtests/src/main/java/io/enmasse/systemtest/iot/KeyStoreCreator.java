/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;

public final class KeyStoreCreator {

    public static final String CERTIFICATE_ALIAS = "cert";
    public static final String KEY_ALIAS = "key";
    public static final String KEY_PASSWORD = "123456";

    private KeyStoreCreator() {}

    public static KeyStore from(final PrivateKey key, final Certificate... certificates) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        return from(Optional.empty(), key, certificates);
    }

    public static KeyStore from(final Optional<String> entryPassword, final PrivateKey key, final Certificate... certificates) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        final KeyStore result = KeyStore.getInstance("PKCS12");
        result.load(null, null);
        result.setKeyEntry(KEY_ALIAS, key, entryPassword.map(String::toCharArray).orElse(null), certificates);
        return result;

    }

    public static void writeTo(final OutputStream stream, final PrivateKey key, final Certificate... certificates)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        // we set both the entry and the keystore password here
        // the keystore password is required by the keystore implementation
        // the entry password is required due to: https://github.com/eclipse-vertx/vert.x/issues/3452

        from(Optional.of(KEY_PASSWORD), key, certificates)
                .store(stream, KEY_PASSWORD.toCharArray());

    }

    public static byte[] toByteArray(final PrivateKey key, final X509Certificate... certificates)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTo(out, key, certificates);
        return out.toByteArray();

    }

}
