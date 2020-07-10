/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"crypto/tls"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/accesscontrolserver"
	serverAmqp "github.com/enmasseproject/enmasse/pkg/accesscontrolserver/amqp"
	"github.com/enmasseproject/enmasse/pkg/util"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"
)

/*
access-control-server - presents a AMQP SASL server that implements CRD driven authentication and authorization.

For development purposes, you can run the access-control-server backend outside the container.  See the Makefile target 'run'.
*/

type logWriter struct {
}

func (writer logWriter) Write(bytes []byte) (int, error) {
	return fmt.Print(time.Now().UTC().Format("2006-01-02T15:04:05.999Z") + " " + string(bytes))
}

func main() {
	log.SetFlags(0)
	log.SetOutput(new(logWriter))

	sigs := make(chan os.Signal, 1)
	done := make(chan bool, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	port := uint16(util.GetUintEnvOrDefault("PORT", 0, 16, 0))
	bindAddress := util.GetEnvOrDefault("BIND_ADDRESS", "localhost")
	tlsCertFile := util.GetEnvOrDefault("TLS_CERT_FILE", "")
	tlsKeyFile := util.GetEnvOrDefault("TLS_KEY_FILE", "")

	// TODO watch the files and restart the listener in the event of change(s) in order to support certificate rotation..
	var tlsConfig *tls.Config
	if tlsCertFile != "" && tlsKeyFile != "" {
		cer, err := tls.LoadX509KeyPair(tlsCertFile, tlsKeyFile)
		if err != nil {
			log.Panic(err)
		}

		tlsConfig = &tls.Config{Certificates: []tls.Certificate{cer}}
	}

	server, err := accesscontrolserver.NewServer(tlsConfig, bindAddress, port, serverAmqp.WithSASLAnonymous())
	if err != nil {
		log.Panic(err)
	}
	log.Printf("Listening : %s TLS : %t", server.GetAddress(), tlsConfig != nil)

	go func() {
		_ = <-sigs
		log.Println("Shutting down")
		server.Stop()
		done <- true
	}()

	<-done
	log.Println("Done")
}
