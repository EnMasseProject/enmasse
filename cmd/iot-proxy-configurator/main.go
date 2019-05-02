/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"flag"
	"os"

	"github.com/enmasseproject/enmasse/pkg/logs"

	enmasse "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"

	"time"

	enmassescheme "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/scheme"
	informers "github.com/enmasseproject/enmasse/pkg/client/informers/externalversions"

	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/runtime/signals"
)

var (
	ephermalCertBase string
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

	if ephermalCertBase != "" {
		fi, err := os.Stat(ephermalCertBase)
		if err != nil {
			log.Error(err, "Ephermal certificate base is configured, but unable to access: %v", err.Error())
			os.Exit(1)
		}
		if !fi.IsDir() {
			log.Info("Ephermal certificate base is configured, but is not a directory")
			os.Exit(1)
		}
	}

	cfg, err := rest.InClusterConfig()
	if err != nil {
		log.Error(err, "Error getting in-cluster config")
		os.Exit(1)
	}

	kubeClient, err := kubernetes.NewForConfig(cfg)
	if err != nil {
		log.Error(err, "Error building kubernetes client")
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

	enmasseInformerFactory := informers.NewSharedInformerFactory(enmasseClient, time.Second*30)

	configurator := NewConfigurator(
		kubeClient, enmasseClient,
		enmasseInformerFactory.Iot().V1alpha1().IoTProjects(),
		ephermalCertBase,
	)

	enmasseInformerFactory.Start(stopCh)

	if err = configurator.Run(1, stopCh); err != nil {
		log.Error(err, "Failed running configurator")
	}
}

func init() {

	ephermalCertBase = "/var/qdr-certs"

	if value, present := os.LookupEnv("EPHERMAL_CERT_BASE"); present {
		ephermalCertBase = value
	}

}
