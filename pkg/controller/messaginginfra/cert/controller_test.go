/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cert

import (
	"testing"
	"time"

	"crypto/x509"
	"encoding/pem"

	pkcs12 "software.sslmate.com/src/go-pkcs12"

	"github.com/stretchr/testify/assert"

	logrtesting "github.com/go-logr/logr/testing"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/scheme"

	"sigs.k8s.io/controller-runtime/pkg/client/fake"
)

func setup(t *testing.T, caExpiration time.Duration, certExpiration time.Duration) *CertController {
	s := scheme.Scheme
	cl := fake.NewFakeClientWithScheme(s)
	return NewCertController(cl, s, caExpiration, certExpiration)
}

func TestCaNew(t *testing.T) {
	cc := setup(t, 24*time.Hour, 24*time.Hour)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "ca"},
	}
	err := cc.applyCaSecret(secret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")

	key := secret.Data["tls.key"]
	crt := secret.Data["tls.crt"]

	// Apply again and verify nothing is changed since certificate should not yet be renewed
	err = cc.applyCaSecret(secret, logrtesting.TestLogger{})
	assert.Nil(t, err)
	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")

	assert.Equal(t, key, secret.Data["tls.key"])
	assert.Equal(t, crt, secret.Data["tls.crt"])
}

func TestCaRenewed(t *testing.T) {
	cc := setup(t, 1*time.Second, 1*time.Second)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "ca"},
	}
	err := cc.applyCaSecret(secret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")

	key := secret.Data["tls.key"]
	crt := secret.Data["tls.crt"]

	time.Sleep(10 * time.Second)

	// Apply again and verify cert has been renewed
	err = cc.applyCaSecret(secret, logrtesting.TestLogger{})
	assert.Nil(t, err)
	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")

	assert.Equal(t, key, secret.Data["tls.key"])
	assert.NotEqual(t, crt, secret.Data["tls.crt"])
}

func TestCertNew(t *testing.T) {
	cc := setup(t, 24*time.Hour, 24*time.Hour)

	caSecret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "ca"},
	}
	err := cc.applyCaSecret(caSecret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "cert1"},
	}

	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a.example.com", "b.example.com")
	assert.Nil(t, err)

	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")
	assert.Contains(t, secret.Data, "ca.crt")
	assert.Contains(t, secret.Data, "keystore.p12")
	assert.Equal(t, caSecret.Data["tls.crt"], secret.Data["ca.crt"])

	// Check that we can decode the valid keystore
	_, _, _, err = pkcs12.DecodeChain(secret.Data["keystore.p12"], "enmasse")
	assert.Nil(t, err)

	// Check that we can decode the valid truststore
	_, err = pkcs12.DecodeTrustStore(secret.Data["truststore.p12"], "enmasse")
	assert.Nil(t, err)

	// Ensure running it again does not change anything
	key := secret.Data["tls.key"]
	crt := secret.Data["tls.crt"]
	p12 := secret.Data["keystore.p12"]

	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a.example.com", "b.example.com")
	assert.Nil(t, err)
	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")
	assert.Contains(t, secret.Data, "ca.crt")
	assert.Contains(t, secret.Data, "keystore.p12")

	assert.Equal(t, caSecret.Data["tls.crt"], secret.Data["ca.crt"])
	assert.Equal(t, crt, secret.Data["tls.crt"])
	assert.Equal(t, key, secret.Data["tls.key"])
	assert.Equal(t, p12, secret.Data["keystore.p12"])
}

func TestCertRenewed(t *testing.T) {
	cc := setup(t, 24*time.Hour, 10*time.Second)

	caSecret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "ca"},
	}
	err := cc.applyCaSecret(caSecret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "cert1"},
	}

	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a.example.com", "b.example.com")
	assert.Nil(t, err)

	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")
	assert.Contains(t, secret.Data, "ca.crt")
	assert.Contains(t, secret.Data, "keystore.p12")
	assert.Equal(t, caSecret.Data["tls.crt"], secret.Data["ca.crt"])
	assertCert(t, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", false)
	assertCert(t, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "b.example.com", false)
	assertCert(t, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "c.example.com", true)

	key := secret.Data["tls.key"]
	crt := secret.Data["tls.crt"]
	p12 := secret.Data["keystore.p12"]

	time.Sleep(10 * time.Second)

	// Certificate should be renewed
	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a.example.com", "b.example.com")
	assert.Nil(t, err)
	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")
	assert.Contains(t, secret.Data, "ca.crt")
	assert.Contains(t, secret.Data, "keystore.p12")
	assert.Equal(t, caSecret.Data["tls.crt"], secret.Data["ca.crt"])
	assertCert(t, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", false)
	assertCert(t, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "b.example.com", false)
	assertCert(t, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "c.example.com", true)

	assert.NotEqual(t, crt, secret.Data["tls.crt"])
	assert.Equal(t, key, secret.Data["tls.key"])
	assert.NotEqual(t, p12, secret.Data["keystore.p12"])
}

func TestCertCaRenewed(t *testing.T) {
	cc := setup(t, 10*time.Second, 10*time.Hour)

	caSecret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "ca"},
	}
	err := cc.applyCaSecret(caSecret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "cert1"},
	}

	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a.example.com", "b.example.com")
	assert.Nil(t, err)

	time.Sleep(10 * time.Second)

	// Verify that certificate is not valid anymore
	assertCert(t, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", true)

	// Renew CA
	err = cc.applyCaSecret(caSecret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	// Our cert is still valid
	assertCert(t, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", false)

	// Renew our cert
	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a.example.com", "b.example.com")

	// Valid again
	assertCert(t, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", false)
}

func assertCert(t *testing.T, caBytes []byte, certBytes []byte, dnsName string, shouldErr bool) {
	roots := x509.NewCertPool()
	ok := roots.AppendCertsFromPEM(caBytes)
	assert.True(t, ok)

	block, _ := pem.Decode(certBytes)
	assert.NotNil(t, block)

	cert, err := x509.ParseCertificate(block.Bytes)
	assert.Nil(t, err)

	opts := x509.VerifyOptions{
		DNSName: dnsName,
		Roots:   roots,
	}

	_, err = cert.Verify(opts)
	if shouldErr {
		assert.NotNil(t, err)
	} else {
		assert.Nil(t, err)
	}
}
