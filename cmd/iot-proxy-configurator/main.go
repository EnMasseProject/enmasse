/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"flag"
	"os"
	"strconv"

	"github.com/enmasseproject/enmasse/pkg/logs"

	enmasse "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"

	"time"

	enmassescheme "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/scheme"
	informers "github.com/enmasseproject/enmasse/pkg/client/informers/externalversions"

	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/runtime/signals"
)

var log = logf.Log.WithName("cmd")

func main() {

	// init log system

	flag.Parse()

	logs.InitLog()
	logs.PrintVersions(log)

	// start processing

	stopCh := signals.SetupSignalHandler()

	log.Info("Starting up...")

	cfg, err := rest.InClusterConfig()
	if err != nil {
		log.Error(err, "Error getting in-cluster config")
		os.Exit(1)
	}

	enmasseClient, err := enmasse.NewForConfig(cfg)
	if err != nil {
		log.Error(err, "Error building EnMasse client")
		os.Exit(1)
	}

	if err := enmassescheme.AddToScheme(scheme.Scheme); err != nil {
		log.Error(err, "Failed to register EnMasse schema")
		os.Exit(1)
	}

	enmasseInformerFactory := informers.NewSharedInformerFactory(enmasseClient, refreshPeriod())

	configurator := NewConfigurator(
		enmasseClient,
		enmasseInformerFactory.Iot().V1().IoTTenants(),
	)

	enmasseInformerFactory.Start(stopCh)

	if err = configurator.Run(1, stopCh); err != nil {
		log.Error(err, "Failed running configurator")
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
