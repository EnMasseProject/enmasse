/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package upgrader

import (
	"github.com/enmasseproject/enmasse/pkg/util"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
)

var log = logf.Log.WithName("upgrader")

type Upgrader struct {
	client    *kubernetes.Clientset
	config    *rest.Config
	scheme    *runtime.Scheme
	namespace string
}

func New(mgr manager.Manager) (*Upgrader, error) {
	client, err := kubernetes.NewForConfig(mgr.GetConfig())
	if err != nil {
		log.Error(err, "Error creating kubernetes client")
		return nil, err
	}
	return &Upgrader{
		client:    client,
		config:    mgr.GetConfig(),
		scheme:    mgr.GetScheme(),
		namespace: util.GetEnvOrDefault("NAMESPACE", "enmasse-infra"),
	}, nil
}

func (u *Upgrader) Upgrade() error {
	return nil
}
