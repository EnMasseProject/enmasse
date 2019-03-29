/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"flag"
	"fmt"
	"os"
	"runtime"

	enmasse "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
	"k8s.io/klog"

	"time"

	enmassescheme "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/scheme"
	informers "github.com/enmasseproject/enmasse/pkg/client/informers/externalversions"

	"sigs.k8s.io/controller-runtime/pkg/runtime/signals"
)

var (
	ephermalCertBase string
)

func printVersion() {
	klog.Info(fmt.Sprintf("Go Version: %s", runtime.Version()))
	klog.Info(fmt.Sprintf("Go OS/Arch: %s/%s", runtime.GOOS, runtime.GOARCH))
}

func initLog() {

	// private copy of klog flags, as they collide with glog and would panic

	var flags flag.FlagSet
	klog.InitFlags(&flags)

	// parse main arguments

	flag.Parse()

	// copy over verbose flag from main app to klog

	v := os.Getenv("VERBOSE")

	// if we have a setting, copy over to klog flags

	if v != "" {
		if err := flags.Set("v", v); err != nil {
			klog.Fatalf("klog init: Failed to set log verbosity: %v", err.Error())
		}
	}

	// setup klog output

	klog.SetOutput(os.Stdout)
}

func main() {

	// init log system

	initLog()

	printVersion()

	// start processing

	stopCh := signals.SetupSignalHandler()

	klog.Infof("Starting up...")

	if ephermalCertBase != "" {
		fi, err := os.Stat(ephermalCertBase)
		if err != nil {
			klog.Fatalf("Ephermal certificate base is configured, but unable to access: %v", err.Error())
		}
		if !fi.IsDir() {
			klog.Fatalln("Ephermal certificate base is configured, but is not a directory")
		}
	}

	cfg, err := rest.InClusterConfig()
	if err != nil {
		klog.Fatalf("Error getting in-cluster config: %v", err.Error())
		os.Exit(1)
	}

	kubeClient, err := kubernetes.NewForConfig(cfg)
	if err != nil {
		klog.Fatalf("Error building kubernetes client: %v", err.Error())
		os.Exit(1)
	}

	enmasseClient, err := enmasse.NewForConfig(cfg)
	if err != nil {
		klog.Fatalf("Error building EnMasse client: %v", err.Error())
		os.Exit(1)
	}

	if err := enmassescheme.AddToScheme(scheme.Scheme); err != nil {
		klog.Fatalf("Failed to register EnMasse schema: %v", err.Error())
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
		klog.Fatalf("Failed running configurator: %v", err.Error())
	}
}

func init() {

	ephermalCertBase = "/var/qdr-certs"

	if value, present := os.LookupEnv("EPHERMAL_CERT_BASE"); present {
		ephermalCertBase = value
	}

}
