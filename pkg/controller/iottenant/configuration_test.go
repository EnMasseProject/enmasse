/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iottenant

import (
	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/stretchr/testify/assert"
	"golang.org/x/net/context"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/record"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"testing"
)

const EC_CA_PEM_1 = `-----BEGIN CERTIFICATE-----
MIIBlzCCATygAwIBAgIBADAKBggqhkjOPQQDAjAqMQwwCgYDVQQDDANmb28xDDAK
BgNVBAsMA2JhcjEMMAoGA1UECgwDYmF6MB4XDTIwMDYxMDEzMjQwMVoXDTIxMDYx
MDEzMjQwMVowKjEMMAoGA1UEAwwDZm9vMQwwCgYDVQQLDANiYXIxDDAKBgNVBAoM
A2JhejBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNX8QhvVNIkF1GRSIv9zjDVM
xPtG8EkIEN9ARW7YB3Y4l/wv4AJSPLFoWQQgfamErFcfB9zO2c/ryvfTVY4i6wKj
UzBRMB0GA1UdDgQWBBQASe3I4LZRilRaJqlomFPeBUOURzAfBgNVHSMEGDAWgBQA
Se3I4LZRilRaJqlomFPeBUOURzAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMC
A0kAMEYCIQDMaLN9bJZkxi4Z8M41XKgXDxH0sEcuus9rBDfZW98toQIhAIE7OZNr
vvC2C0U0wwb/Q72yIYU+nm5RbT5XqkXAW6Fi
-----END CERTIFICATE-----
`

const EC_CA_PEM_2 = `-----BEGIN CERTIFICATE-----
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
-----END CERTIFICATE-----
`

func setup(t *testing.T) *ReconcileIoTTenant {
	s := scheme.Scheme

	s.AddKnownTypes(iotv1.SchemeGroupVersion, &iotv1.IoTTenant{})
	s.AddKnownTypes(iotv1.SchemeGroupVersion, &iotv1.IoTInfrastructure{})

	var objs []runtime.Object

	c := fake.NewFakeClientWithScheme(s, objs...)

	return &ReconcileIoTTenant{
		client:   c,
		reader:   c,
		scheme:   s,
		recorder: record.NewFakeRecorder(100),
	}

}

// ensure that assigning a trust anchor, claims the name
func TestEcCertClaim(t *testing.T) {

	r := setup(t)

	// dummy tenant

	tenant := iotv1.IoTTenant{}

	// EC based trust anchor

	trustAnchor := iotv1.TrustAnchor{
		Certificate: EC_CA_PEM_1,
	}

	// result

	accepted := make(map[string]bool)
	duplicate := make(map[string]bool)

	// setup namespace for testing

	util.SetInfrastructureNamespaceForTesting("enmasse-infra")

	// call method

	ata, err := r.acceptTrustAnchor(&tenant, trustAnchor, accepted, duplicate)

	// expect no error

	assert.Nil(t, err, "Failed to accept trust anchor")

	// expected PEM to be parsed

	assert.Equal(t, "EC", ata.Algorithm)
	assert.Equal(t, "O=baz,OU=bar,CN=foo", ata.SubjectDN)
	assert.Equal(t, "2020-06-10 13:24:01 +0000 UTC", ata.NotBefore.String())
	assert.Equal(t, "2021-06-10 13:24:01 +0000 UTC", ata.NotAfter.String())

	// expect to set claim

	cm := corev1.ConfigMap{}
	err = r.client.Get(context.Background(), client.ObjectKey{
		Namespace: "enmasse-infra",
		Name:      lockMapForSubjectDn(ata.SubjectDN),
	}, &cm)
	assert.Nil(t, err, "Failed to get trust anchor claim")

	assert.True(t, isLockMapOwnedByTenant(&cm, &tenant))

}

// ensure that the naming between Java and Go is consistent
func TestEcCertName(t *testing.T) {

	r := setup(t)

	// dummy tenant

	tenant := iotv1.IoTTenant{}

	// EC based trust anchor

	trustAnchor := iotv1.TrustAnchor{
		Certificate: EC_CA_PEM_2,
	}

	// result

	accepted := make(map[string]bool)
	duplicate := make(map[string]bool)

	// setup namespace for testing

	util.SetInfrastructureNamespaceForTesting("enmasse-infra")

	// call method

	ata, err := r.acceptTrustAnchor(&tenant, trustAnchor, accepted, duplicate)

	// expect no error

	assert.Nil(t, err, "Failed to accept trust anchor")

	// expected PEM to be parsed

	assert.Equal(t, "EC", ata.Algorithm)
	assert.Equal(t, "OU=Tenant 1,OU=IoT,O=EnMasse,C=IO", ata.SubjectDN)
	assert.Equal(t, "2020-06-16 09:01:53 +0000 UTC", ata.NotBefore.String())
	assert.Equal(t, "2021-06-16 09:01:53 +0000 UTC", ata.NotAfter.String())

	// expect to set claim

	cm := corev1.ConfigMap{}
	err = r.client.Get(context.Background(), client.ObjectKey{
		Namespace: "enmasse-infra",
		Name:      lockMapForSubjectDn(ata.SubjectDN),
	}, &cm)
	assert.Nil(t, err, "Failed to get trust anchor claim")

	assert.True(t, isLockMapOwnedByTenant(&cm, &tenant))

}
