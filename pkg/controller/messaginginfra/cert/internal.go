/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cert

import (
	"bytes"
	"errors"
	"time"

	"encoding/pem"

	"math/big"

	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"crypto/x509/pkix"

	pkcs12 "software.sslmate.com/src/go-pkcs12"
)

func getCertExpiryDate(certBytes []byte) (time.Time, error) {
	cert, err := parsePemCertificate(certBytes)
	if err != nil {
		return time.Time{}, err
	}

	return cert.NotAfter, nil
}

func generateCa(caKey []byte, caCert []byte, expiryDate time.Time) ([]byte, []byte, error) {
	var err error
	var caPrivateKey *rsa.PrivateKey
	if caKey == nil {
		caPrivateKey, err = rsa.GenerateKey(rand.Reader, 4096)
		if err != nil {
			return nil, nil, err
		}
	} else {
		caPrivateKey, err = parsePemKey(caKey)
		if err != nil {
			return nil, nil, err
		}
	}

	serialNumber := big.NewInt(2020)
	if caCert != nil {
		existingCa, err := parsePemCertificate(caCert)
		if err != nil {
			return nil, nil, err
		}
		serialNumber = existingCa.SerialNumber.Add(existingCa.SerialNumber, big.NewInt(1))
	}

	ca := &x509.Certificate{
		SerialNumber: serialNumber,
		Subject: pkix.Name{
			Organization:  []string{"EnMasse"},
			Country:       []string{"US"},
			Province:      []string{""},
			Locality:      []string{""},
			StreetAddress: []string{""},
			PostalCode:    []string{""},
		},
		NotBefore:             time.Now(),
		NotAfter:              expiryDate,
		IsCA:                  true,
		ExtKeyUsage:           []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth, x509.ExtKeyUsageServerAuth},
		KeyUsage:              x509.KeyUsageDigitalSignature | x509.KeyUsageCertSign,
		BasicConstraintsValid: true,
	}

	caBytes, err := x509.CreateCertificate(rand.Reader, ca, ca, &caPrivateKey.PublicKey, caPrivateKey)
	if err != nil {
		return nil, nil, err
	}

	encodedPemKey, err := encodePemKey(caPrivateKey)
	if err != nil {
		return nil, nil, err
	}
	encodedPemCert, err := encodePemCert(caBytes)
	if err != nil {
		return nil, nil, err
	}
	return encodedPemKey, encodedPemCert, nil
}

func generateCert(keyPem []byte, certPem []byte, caKeyPem []byte, caCertPem []byte, expiryDate time.Time, dnsNames []string) ([]byte, []byte, []byte, []byte, error) {
	caKey, err := parsePemKey(caKeyPem)
	if err != nil {
		return nil, nil, nil, nil, err
	}

	caCert, err := parsePemCertificate(caCertPem)
	if err != nil {
		return nil, nil, nil, nil, err
	}

	var certPrivateKey *rsa.PrivateKey
	if keyPem == nil {
		certPrivateKey, err = rsa.GenerateKey(rand.Reader, 4096)
		if err != nil {
			return nil, nil, nil, nil, err
		}
	} else {
		certPrivateKey, err = parsePemKey(keyPem)
		if err != nil {
			return nil, nil, nil, nil, err
		}
	}

	serialNumber := big.NewInt(2020)
	if certPem != nil {
		existingCert, err := parsePemCertificate(certPem)
		if err != nil {
			return nil, nil, nil, nil, err
		}
		serialNumber = existingCert.SerialNumber.Add(existingCert.SerialNumber, big.NewInt(1))
	}

	cert := &x509.Certificate{
		SerialNumber: serialNumber,
		Subject: pkix.Name{
			Organization:  []string{"EnMasse"},
			Country:       []string{"US"},
			Province:      []string{""},
			Locality:      []string{""},
			StreetAddress: []string{""},
			PostalCode:    []string{""},
		},
		NotBefore:             time.Now(),
		NotAfter:              expiryDate,
		DNSNames:              dnsNames,
		IsCA:                  false,
		ExtKeyUsage:           []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth, x509.ExtKeyUsageServerAuth},
		SubjectKeyId:          []byte{1, 2, 3, 4, 6},
		KeyUsage:              x509.KeyUsageDigitalSignature,
		BasicConstraintsValid: true,
	}

	certBytes, err := x509.CreateCertificate(rand.Reader, cert, caCert, &certPrivateKey.PublicKey, caKey)
	if err != nil {
		return nil, nil, nil, nil, err
	}

	c, err := x509.ParseCertificate(certBytes)
	if err != nil {
		return nil, nil, nil, nil, err
	}

	keyStore, err := pkcs12.Encode(rand.Reader, certPrivateKey, c, []*x509.Certificate{caCert}, "enmasse")
	if err != nil {
		return nil, nil, nil, nil, err
	}

	trustStore, err := pkcs12.EncodeTrustStore(rand.Reader, []*x509.Certificate{caCert}, "enmasse")
	if err != nil {
		return nil, nil, nil, nil, err
	}

	encodedPemKey, err := encodePemKey(certPrivateKey)
	if err != nil {
		return nil, nil, nil, nil, err
	}
	encodedPemCert, err := encodePemCert(certBytes)
	if err != nil {
		return nil, nil, nil, nil, err
	}

	return encodedPemKey, encodedPemCert, keyStore, trustStore, nil
}

func parsePemCertificate(data []byte) (*x509.Certificate, error) {
	block, _ := pem.Decode(data)
	if block == nil {
		return nil, errors.New("Failed to parse PEM certificate")
	}

	cert, err := x509.ParseCertificate(block.Bytes)
	return cert, err
}

func parsePemKey(data []byte) (*rsa.PrivateKey, error) {
	block, _ := pem.Decode(data)
	if block == nil {
		return nil, errors.New("Failed to parse PEM private key")
	}

	key, err := x509.ParsePKCS1PrivateKey(block.Bytes)
	return key, err
}

func encodePemKey(key *rsa.PrivateKey) ([]byte, error) {
	certPrivateKeyPem := new(bytes.Buffer)
	err := pem.Encode(certPrivateKeyPem, &pem.Block{
		Type:  "RSA PRIVATE KEY",
		Bytes: x509.MarshalPKCS1PrivateKey(key),
	})
	if err != nil {
		return nil, err
	}
	return certPrivateKeyPem.Bytes(), nil
}

func encodePemCert(certBytes []byte) ([]byte, error) {
	certPem := new(bytes.Buffer)
	err := pem.Encode(certPem, &pem.Block{
		Type:  "CERTIFICATE",
		Bytes: certBytes,
	})
	if err != nil {
		return nil, err
	}

	return certPem.Bytes(), nil
}
