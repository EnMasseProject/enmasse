/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"crypto/x509"
	"encoding/pem"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestSimpleName(t *testing.T) {
	pemCert := `-----BEGIN CERTIFICATE-----
MIIBwTCCAWigAwIBAgIBADAKBggqhkjOPQQDAjBAMQswCQYDVQQGEwJJTzEQMA4G
A1UEChMHRW5NYXNzZTEMMAoGA1UECxMDSW9UMREwDwYDVQQLEwhUZW5hbnQgMTAe
Fw0yMDA2MTYwOTAxNTNaFw0yMTA2MTYwOTAxNTNaMEAxCzAJBgNVBAYTAklPMRAw
DgYDVQQKEwdFbk1hc3NlMQwwCgYDVQQLEwNJb1QxETAPBgNVBAsTCFRlbmFudCAx
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE4tyVux5bOdKPD/eWTj8NGV6hr4AL
KX6eB84pyGMTOvmRZkGZPrwVEGwPCtnh/lZ3TJHGNbBYX8BIg91pUtACpaNTMFEw
HQYDVR0OBBYEFGtqAWo7c2bc9FsMunL3qW/2wJCQMB8GA1UdIwQYMBaAFGtqAWo7
c2bc9FsMunL3qW/2wJCQMA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDRwAw
RAIgHyi3rI1nx/14sXQMFxnQmfKL0MXna+KqPgGFcATU1wICIAodp3lwO2OO5zPB
c0/MYxV/W7QNg/WV/Om7uqmTXQO3
-----END CERTIFICATE-----`

	block, _ := pem.Decode([]byte(pemCert))
	cert, _ := x509.ParseCertificate(block.Bytes)

	name, err := ToX500Name(cert.RawSubject)
	assert.Nil(t, err, "No error should occur when decoding name")
	assert.Equal(t, "OU=Tenant 1,OU=IoT,O=EnMasse,C=IO", name)
}
