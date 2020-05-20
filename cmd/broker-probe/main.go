/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"log"
	"os"
	"time"

	"pack.ag/amqp"
)

func main() {
	probeAddress := os.Getenv("PROBE_ADDRESS")
	if probeAddress == "" {
		log.Fatal("Error looking up PROBE_ADDRESS")
	}
	log.Println("Found probe address, connecting")

	username := os.Getenv("PROBE_USERNAME")
	if username == "" {
		username = "probe"
	}

	password := os.Getenv("PROBE_PASSWORD")
	if password == "" {
		password = "probe"
	}

	probeTimeoutStr := os.Getenv("PROBE_TIMEOUT")
	if probeTimeoutStr == "" {
		probeTimeoutStr = "2s"
	}

	probeTimeout, err := time.ParseDuration(probeTimeoutStr)
	if err != nil {
		log.Fatal(err, "Error parsing duration")
	}

	done := make(chan error)
	go func() {
		done <- runProbe("amqp://127.0.0.1:5672", probeAddress, username, password, probeTimeout)
	}()

	select {
	case <-time.After(probeTimeout):
		os.Exit(1)
	case err := <-done:
		if err != nil {
			os.Exit(1)
		}
	}
}

func runProbe(url, probeAddress, username, password string, probeTimeout time.Duration) error {
	client, err := amqp.Dial(url, amqp.ConnSASLPlain(username, password), amqp.ConnConnectTimeout(probeTimeout), amqp.ConnProperty("product", "broker-probe"))
	if err != nil {
		log.Println(err, "Error dialing broker")
		return err
	}
	log.Println("Connected, creating session")

	defer func() {
		_ = client.Close()
	}()

	session, err := client.NewSession()
	if err != nil {
		log.Println(err, "Error creating session")
		return err
	}

	log.Println("Session created")

	_, err = session.NewReceiver(amqp.LinkAddress(probeAddress))
	if err != nil {
		log.Println(err, "Error attaching link")
		return err
	}

	log.Println("Link created")
	return nil
}
