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

public final class KeyStoreCreator {

    public static final String CERTIFICATE_ALIAS = "cert";
    public static final String KEY_ALIAS = "key";
    public static final String KEY_PASSWORD = "123456";

    private KeyStoreCreator() {}

    public static KeyStore from(final PrivateKey key, final Certificate ...certificate) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException  {

        final KeyStore result = KeyStore.getInstance("PKCS12");
        result.load(null, null);
        result.setKeyEntry(KEY_ALIAS, key, null, certificate);
        return result;

    }

    public static void writeTo (final OutputStream stream, final PrivateKey key, final Certificate ...certificate) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        from(key, certificate).store(stream, KEY_ALIAS.toCharArray());
    }

    public static byte[] toByteArray(final PrivateKey key, final X509Certificate certificate) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTo(out, key, certificate);
        return out.toByteArray();
    }

}
