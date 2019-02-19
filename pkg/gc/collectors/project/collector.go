/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package project

import (
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned"
	"github.com/enmasseproject/enmasse/pkg/gc/collectors"
	"github.com/enmasseproject/enmasse/pkg/util"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
)

var log = logf.Log.WithName("project_collector")

// test if interface is implemented
var _ collectors.Collector = &projectCollector{}

type projectCollector struct {
	client    *versioned.Clientset
	namespace string
}

func NewProjectCollector(enmasseClient *versioned.Clientset, namespace string) *projectCollector {
	return &projectCollector{
		client:    enmasseClient,
		namespace: namespace,
	}
}

// Collect once
func (p *projectCollector) CollectOnce() error {

	mt := util.MultiTool{}

	mt.Ran(p.collectAddressSpaces())
	mt.Ran(p.collectAddresses())
	mt.Ran(p.collectMessagingUsers())

	return mt.Error
}
