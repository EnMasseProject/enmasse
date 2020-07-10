/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package accesscontrolserver

import (
	"crypto/tls"
	serverAmqp "github.com/enmasseproject/enmasse/pkg/accesscontrolserver/amqp"
	"github.com/stretchr/testify/assert"
	"k8s.io/client-go/util/cert"
	"net"
	"os"
	"pack.ag/amqp"
	"syscall"
	"testing"
	"time"
)

var clientConnTimeout = amqp.ConnConnectTimeout(10 * time.Second)
var clientSaslAnon = amqp.ConnSASLAnonymous()
var clientInsecureTls = amqp.ConnTLSConfig(&tls.Config{
	InsecureSkipVerify: true,
})

func TestServerStartStop(t *testing.T) {
	server, err := NewServer(nil, "localhost", 0)
	assert.NoError(t, err)
	server.Stop()
}

func TestSaslAnonymous(t *testing.T) {
	server, err := NewServer(nil, "localhost", 0, serverAmqp.WithSASLAnonymous())
	assert.NoError(t, err)
	defer server.Stop()
	addr := "amqp://" + server.GetAddress()
	client, err := amqp.Dial(addr, clientSaslAnon, clientConnTimeout)
	assert.NoError(t, err)
	if client != nil {
		defer client.Close()
	}

	authIdValue, ok := client.PeerProperties()["authenticated-identity"]
	assert.True(t, ok)
	assert.Implements(t, (map[string]interface{})(nil), authIdValue)
	authIdMap := authIdValue.(map[string]interface{})
	assert.Equal(t, "anonymous", authIdMap["sub"])
	assert.Equal(t, "anonymous", authIdMap["preferred_username"])

	groupsValue, ok := client.PeerProperties()["groups"]
	assert.True(t, ok)
	groupsList := groupsValue.([]string)
	assert.ElementsMatch(t, []string{"manage"}, groupsList)
}

func TestNoSaslRefused(t *testing.T) {
	server, err := NewServer(nil, "localhost", 0, serverAmqp.WithSASLAnonymous())
	assert.NoError(t, err)
	defer server.Stop()
	addr := "amqp://" + server.GetAddress()
	client, err := amqp.Dial(addr, clientConnTimeout)
	assert.NoError(t, err)
	// Provoke an error by using the connection
	_, err = client.NewSession()
	assert.Error(t, err, "AMQP protocol 0 unexpected at this time - AMQP SASL is required first")
	if client != nil {
		defer client.Close()
	}

	_, authIdOk := client.PeerProperties()["authenticated-identity"]
	assert.False(t, authIdOk)
	_, groupsOk := client.PeerProperties()["groups"]
	assert.False(t, groupsOk)
}

func TestSlowConnectionTimeout(t *testing.T) {
	server, err := NewServer(nil, "localhost", 0, serverAmqp.WithSASLAnonymous(), serverAmqp.WithConnTimeout(100*time.Millisecond))
	assert.NoError(t, err)
	defer server.Stop()

	dialer := &net.Dialer{Timeout: time.Minute * 1}
	raw, err := dialer.Dial("tcp", server.GetAddress())
	assert.NoError(t, err)
	defer raw.Close()
	_, err = raw.Write([]byte("A"))

	trickle := []byte("AMQP")
	i := 0
	assert.NoError(t, err)
	assert.Eventually(t, func() bool {
		_, err = raw.Write(trickle[i : i+1])
		i++
		return isBrokenPipe(err)
	}, time.Minute*1, time.Millisecond*500)
}

func TestTlsSaslAnonymous(t *testing.T) {
	crt, key, err := cert.GenerateSelfSignedCertKey("localhost", []net.IP{}, []string{})
	assert.NoError(t, err)

	kp, err := tls.X509KeyPair(crt, key)
	assert.NoError(t, err)
	tlsConfig := &tls.Config{Certificates: []tls.Certificate{kp}}

	server, err := NewServer(tlsConfig, "localhost", 0, serverAmqp.WithSASLAnonymous())
	assert.NoError(t, err)
	defer server.Stop()
	addr := "amqps://" + server.GetAddress()
	client, err := amqp.Dial(addr, clientSaslAnon, clientConnTimeout, clientInsecureTls)
	assert.NoError(t, err)
	if client != nil {
		defer client.Close()
	}

	authIdValue, ok := client.PeerProperties()["authenticated-identity"]
	assert.True(t, ok)
	assert.Implements(t, (map[string]interface{})(nil), authIdValue)
	authIdMap := authIdValue.(map[string]interface{})
	assert.Equal(t, "anonymous", authIdMap["sub"])
	assert.Equal(t, "anonymous", authIdMap["preferred_username"])

	groupsValue, ok := client.PeerProperties()["groups"]
	assert.True(t, ok)
	groupsList := groupsValue.([]string)
	assert.ElementsMatch(t, []string{"manage"}, groupsList)
}

func isBrokenPipe(err error) bool {
	if oe, ok := err.(*net.OpError); ok {
		if sce, ok := oe.Err.(*os.SyscallError); ok {
			if sce.Err == syscall.EPIPE || oe.Err == syscall.ECONNRESET {
				return true
			} else {
				return false
			}
		}
	}
	return false
}
