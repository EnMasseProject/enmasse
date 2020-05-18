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

	client, err := amqp.Dial("amqp://127.0.0.1:5672", amqp.ConnSASLPlain(username, password), amqp.ConnConnectTimeout(probeTimeout), amqp.ConnProperty("product", "broker-probe"))
	if err != nil {
		log.Fatal(err, "Error dialing broker")
	}
	log.Println("Connected, creating session")

	defer func() {
		_ = client.Close()
	}()

	session, err := client.NewSession()
	if err != nil {
		log.Fatal(err, "Error creating session")
	}

	log.Println("Session created")

	_, err = session.NewReceiver(amqp.LinkAddress(probeAddress))
	if err != nil {
		log.Fatal(err, "Error attaching link")
	}

	log.Println("Link created")
}
