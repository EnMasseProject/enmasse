/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"flag"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"os"
	"sigs.k8s.io/controller-runtime/pkg/client/config"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"strconv"

	"github.com/enmasseproject/enmasse/pkg/logs"

	"time"

	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/runtime/signals"
)

var (
	ephemeralCertBase string
)

var log = logf.Log.WithName("cmd")

func main() {

	// init log system

	flag.Parse()

	logs.InitLog()
	logs.PrintVersions(log)

	// start processing

	log.Info("Starting up...")

	// Get a config to talk to the apiserver
	cfg, err := config.GetConfig()
	if err != nil {
		log.Error(err, "Failed to get configuration")
		os.Exit(1)
	}

	refresh := refreshPeriod()

	mgr, err := manager.New(cfg, manager.Options{
		SyncPeriod: &refresh,
	})
	if err != nil {
		log.Error(err, "Failed to create manager")
		os.Exit(1)
	}

	if err := iotv1alpha1.AddToScheme(mgr.GetScheme()); err != nil {
		log.Error(err, "Failed to register IoT schema")
		os.Exit(1)
	}

	if err := add(mgr); err != nil {
		log.Error(err, "Failed to create controller")
		os.Exit(1)
	}

	// Start the Cmd
	if err := mgr.Start(signals.SetupSignalHandler()); err != nil {
		log.Error(err, "manager exited non-zero")
		os.Exit(1)
	}
}

const DefaultInformerRefreshPeriod = time.Second * 60

func refreshPeriod() time.Duration {

	if value, present := os.LookupEnv("INFORMER_REFRESH_PERIOD_SECONDS"); present {
		if i, err := strconv.ParseInt(value, 10, 32); err != nil {
			return DefaultInformerRefreshPeriod
		} else {
			return time.Duration(i) * time.Second
		}
	}

	// return default

	return DefaultInformerRefreshPeriod
}

func init() {

	ephemeralCertBase = "/var/qdr-certs"

	if value, present := os.LookupEnv("EPHEMERAL_CERT_BASE"); present {
		ephemeralCertBase = value
	}

}
