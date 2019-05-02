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
	"github.com/enmasseproject/enmasse/pkg/gc"
	"github.com/enmasseproject/enmasse/pkg/gc/collectors/project"
	"github.com/operator-framework/operator-sdk/pkg/k8sutil"
	"k8s.io/klog"
	"sigs.k8s.io/controller-runtime/pkg/client/config"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/runtime/signals"
)

var log = logf.Log.WithName("cmd")

func main() {

	flag.Parse()

	logs.InitLog()
	logs.PrintVersions(log)

	namespace, _ := os.LookupEnv(k8sutil.WatchNamespaceEnvVar)

	stopCh := signals.SetupSignalHandler()

	cfg, err := config.GetConfig()
	if err != nil {
		log.Error(err, "Failed to get configuration")
		os.Exit(1)
	}

	enmasseClient, err := enmasse.NewForConfig(cfg)
	if err != nil {
		klog.Fatalf("Error building EnMasse client: %v", err.Error())
	}

	collectors := gc.NewGarbageCollector()
	collectors.AddCollector(project.NewProjectCollector(enmasseClient, namespace))
	collectors.Run(stopCh)
}
