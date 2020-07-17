/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class DeviceCertificateManager {

    public static class Device {

        private final KeyPair key;
        private final X509Certificate certificate;

        private Device(final KeyPair key, final X509Certificate certificate) {
            this.key = key;
            this.certificate = certificate;
        }

        public KeyPair getKey() {
            return this.key;
        }

        public X509Certificate getCertificate() {
            return this.certificate;
        }

        public String getDeviceId() {
            return this.certificate.getSubjectX500Principal().getName();
        }

    }

    public static enum Mode {
        RSA("RSA", "SHA256withRSA") {
            @Override
            public AlgorithmParameterSpec getSpec() {
                return new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4);
            }
        },
        EC("EC", "SHA256withECDSA") {
            @Override
            public AlgorithmParameterSpec getSpec() {
                return new ECGenParameterSpec("secp256r1");
            }
        },
        ;

        private final String generatorAlgorithm;
        private final String signatureAlgorithm;

        Mode(final String generatorAlgorithm, final String signatureAlgorithm) {
            this.generatorAlgorithm = generatorAlgorithm;
            this.signatureAlgorithm = signatureAlgorithm;
        }

        public abstract AlgorithmParameterSpec getSpec();

        public String getGeneratorAlgorithm() {
            return generatorAlgorithm;
        }

        public String getSignatureAlgorithm() {
            return signatureAlgorithm;
        }
    }

    private final AtomicLong serialNumber = new AtomicLong();

    private final Mode mode;
    private final X500Principal baseName;

    private final KeyPairGenerator keyPairGenerator;
    private final KeyPair keyPair;
    private final X509Certificate certificate;

    public DeviceCertificateManager(final Mode mode, final X500Principal baseName) throws Exception {

        this.mode = mode;
        this.baseName = baseName;
        this.keyPairGenerator = KeyPairGenerator.getInstance(mode.getGeneratorAlgorithm());
        this.keyPairGenerator.initialize(mode.getSpec());
        this.keyPair = keyPairGenerator.generateKeyPair();

        final Instant now = Instant.now();

        final ContentSigner contentSigner = new JcaContentSignerBuilder(mode.getSignatureAlgorithm())
                .build(this.keyPair.getPrivate());

        final X509CertificateHolder certificate = new JcaX509v3CertificateBuilder(
                baseName,
                BigInteger.valueOf(this.serialNumber.getAndIncrement()),
                Date.from(now),
                Date.from(now.plus(Duration.ofDays(365))),
                baseName,
                this.keyPair.getPublic())
                        .addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(this.keyPair.getPublic()))
                        .addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(this.keyPair.getPublic()))
                        .addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
                        .build(contentSigner);

        this.certificate = new JcaX509CertificateConverter()
                .setProvider(new BouncyCastleProvider())
                .getCertificate(certificate);

    }

    public X509Certificate getCertificate() {
        return this.certificate;
    }

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    private static SubjectKeyIdentifier createSubjectKeyId(final PublicKey publicKey) throws OperatorCreationException {

        final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        final DigestCalculator digCalc = new BcDigestCalculatorProvider()
                .get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));

        return new X509ExtensionUtils(digCalc)
                .createSubjectKeyIdentifier(publicKeyInfo);

    }

    private static AuthorityKeyIdentifier createAuthorityKeyId(final PublicKey publicKey)
            throws OperatorCreationException {

        final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        final DigestCalculator digCalc = new BcDigestCalculatorProvider()
                .get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));

        return new X509ExtensionUtils(digCalc)
                .createAuthorityKeyIdentifier(publicKeyInfo);

    }

    public Device createDevice() throws Exception {
        var now = Instant.now();
        return createDevice(Names.randomDevice(), now, now.plus(Duration.ofDays(90)));
    }

    public Device createDevice(final String deviceName, final Instant notBefore, final Duration validFor) throws Exception {
        return createDevice(deviceName, notBefore, notBefore.plus(validFor), null);
    }

    public Device createDevice(final String deviceName, final Instant notBefore, final Instant notAfter) throws Exception {
        return createDevice(deviceName, notBefore, notAfter, null);
    }

    public Device createDevice(final String deviceName, final Instant notBefore, final Instant notAfter, final Consumer<X509v3CertificateBuilder> customizer) throws Exception {

        // create the fill device name

        final X500NameBuilder builder = new X500NameBuilder(RFC4519Style.INSTANCE);
        Arrays
                .asList(new X500Name(this.baseName.getName()).getRDNs())
                .forEach(e -> builder.addMultiValuedRDN(e.getTypesAndValues()));
        builder.addRDN(RFC4519Style.cn, deviceName);
        final X500Principal name = new X500Principal(builder.build().toString());

        // create a new key pair for the device

        final KeyPair deviceKey = this.keyPairGenerator.generateKeyPair();

        // sign certificate with CA key

        final ContentSigner contentSigner = new JcaContentSignerBuilder(mode.getSignatureAlgorithm())
                .build(this.keyPair.getPrivate());

        // create certificate

        final X509v3CertificateBuilder deviceCertificateBuilder = new JcaX509v3CertificateBuilder(
                this.baseName,
                BigInteger.valueOf(this.serialNumber.getAndIncrement()),
                Date.from(notBefore),
                Date.from(notAfter),
                name,
                deviceKey.getPublic())
                        .addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(deviceKey.getPublic()))
                        .addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(this.keyPair.getPublic()));

        // customize

        if (customizer != null) {
            customizer.accept(deviceCertificateBuilder);
        }

        // convert to JCA certificate

        final X509Certificate deviceCertificate = new JcaX509CertificateConverter()
                .setProvider(new BouncyCastleProvider())
                .getCertificate(deviceCertificateBuilder.build(contentSigner));

        // return result

        return new Device(deviceKey, deviceCertificate);

    }

}
