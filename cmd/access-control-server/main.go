/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/util"
	"log"
	"net"
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

	port := uint32(util.GetUintEnvOrDefault("PORT", 0, 32, 0))

	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		panic(err)
	}

	defer listener.Close()

	go func() {
		for {
			nc, err := listener.Accept() // Accept a net.Conn
			if err != nil {
				panic(err)
			}
			nc.Close()
		}
	}()




}
