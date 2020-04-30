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

type testClock struct {
	now time.Time
}

func (t *testClock) Now() time.Time {
	return t.now
}

func setup(t *testing.T) (*CertController, *testClock) {
	s := scheme.Scheme
	cl := fake.NewFakeClientWithScheme(s)
	clock := &testClock{
		now: time.Now(),
	}
	return &CertController{
		client:             cl,
		scheme:             s,
		caExpirationTime:   48 * time.Hour,
		certExpirationTime: 24 * time.Hour,
		clock:              clock,
	}, clock
}

func TestCaNew(t *testing.T) {
	cc, _ := setup(t)

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
	cc, clock := setup(t)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "ca"},
	}
	err := cc.applyCaSecret(secret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")

	key := secret.Data["tls.key"]
	crt := secret.Data["tls.crt"]

	// Fast forward and verify cert has been renewed
	clock.now = clock.now.Add(48 * time.Hour)

	err = cc.applyCaSecret(secret, logrtesting.TestLogger{})
	assert.Nil(t, err)
	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")

	assert.Equal(t, key, secret.Data["tls.key"])
	assert.NotEqual(t, crt, secret.Data["tls.crt"])
}

func TestCertNew(t *testing.T) {
	cc, _ := setup(t)

	caSecret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "ca"},
	}
	err := cc.applyCaSecret(caSecret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "cert1"},
	}

	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a", "a.example.com", "b.example.com")
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

	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a", "a.example.com", "b.example.com")
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
	cc, clock := setup(t)

	caSecret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "ca"},
	}
	err := cc.applyCaSecret(caSecret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "cert1"},
	}

	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a", "a.example.com", "b.example.com")
	assert.Nil(t, err)

	// Add passing of time to ensure cert is valid
	clock.now = clock.now.Add(12 * time.Hour)

	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")
	assert.Contains(t, secret.Data, "ca.crt")
	assert.Contains(t, secret.Data, "keystore.p12")
	assert.Equal(t, caSecret.Data["tls.crt"], secret.Data["ca.crt"])
	assertCert(t, clock, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", false)
	assertCert(t, clock, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "b.example.com", false)
	assertCert(t, clock, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "c.example.com", true)

	key := secret.Data["tls.key"]
	crt := secret.Data["tls.crt"]
	p12 := secret.Data["keystore.p12"]

	clock.now = clock.now.Add(12 * time.Hour)

	// Certificate should be renewed
	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a", "a.example.com", "b.example.com")
	assert.Nil(t, err)

	// Add passing of time to ensure cert is valid
	clock.now = clock.now.Add(12 * time.Hour)

	assert.Contains(t, secret.Data, "tls.key")
	assert.Contains(t, secret.Data, "tls.crt")
	assert.Contains(t, secret.Data, "ca.crt")
	assert.Contains(t, secret.Data, "keystore.p12")
	assert.Equal(t, caSecret.Data["tls.crt"], secret.Data["ca.crt"])
	assertCert(t, clock, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", false)
	assertCert(t, clock, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "b.example.com", false)
	assertCert(t, clock, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "c.example.com", true)

	assert.NotEqual(t, crt, secret.Data["tls.crt"])
	assert.Equal(t, key, secret.Data["tls.key"])
	assert.NotEqual(t, p12, secret.Data["keystore.p12"])
}

func TestCertCaRenewed(t *testing.T) {
	cc, clock := setup(t)

	caSecret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "ca"},
	}
	err := cc.applyCaSecret(caSecret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "cert1"},
	}

	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a", "a.example.com", "b.example.com")
	assert.Nil(t, err)

	clock.now = clock.now.Add(24 * time.Hour)

	// Verify that certificate is not valid anymore
	assertCert(t, clock, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", true)

	// Renew CA
	err = cc.applyCaSecret(caSecret, logrtesting.TestLogger{})
	assert.Nil(t, err)

	// Add passing of time to ensure cert is no longer valid
	clock.now = clock.now.Add(1 * time.Hour)

	// Our cert is not valid
	assertCert(t, clock, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", true)

	// Renew our cert
	err = cc.applyCertSecret(secret, caSecret, logrtesting.TestLogger{}, "a", "a.example.com", "b.example.com")

	// Add passing of time to ensure cert is valid
	clock.now = clock.now.Add(1 * time.Hour)

	// Valid again
	assertCert(t, clock, caSecret.Data["tls.crt"], secret.Data["tls.crt"], "a.example.com", false)
}

func assertCert(t *testing.T, clock Clock, caBytes []byte, certBytes []byte, dnsName string, shouldErr bool) {
	roots := x509.NewCertPool()
	ok := roots.AppendCertsFromPEM(caBytes)
	assert.True(t, ok)

	block, _ := pem.Decode(certBytes)
	assert.NotNil(t, block)

	cert, err := x509.ParseCertificate(block.Bytes)
	assert.Nil(t, err)

	opts := x509.VerifyOptions{
		DNSName:     dnsName,
		Roots:       roots,
		CurrentTime: clock.Now(),
	}

	_, err = cert.Verify(opts)
	if shouldErr {
		assert.NotNil(t, err)
	} else {
		assert.Nil(t, err)
	}
}
