/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

//go:generate go run ../../../hack/generate_resource_watchers.go MessagingProject EnmasseV1Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1 github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1
//go:generate go run ../../../hack/generate_resource_watchers.go MessagingAddress EnmasseV1Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1 github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1
//go:generate go run ../../../hack/generate_resource_watchers.go MessagingAddressPlan EnmasseV1Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1 github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1
//go:generate go run ../../../hack/generate_resource_watchers.go MessagingPlan EnmasseV1Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1 github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1
//go:generate go run ../../../hack/generate_resource_watchers.go MessagingEndpoint EnmasseV1Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1 github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1

//go:generate go run ../../../hack/generate_resource_watchers.go --scope clusterwide Namespace CoreV1Interface k8s.io/client-go/kubernetes/typed/core/v1 k8s.io/api/core/v1

package watchers

import (
	"reflect"
	"sync/atomic"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
)

type ResourceWatcher interface {
	Watch() error
	AwaitWatching()
	Shutdown()
	GetRestartCount() int32
}

type WatcherOption func(watcher ResourceWatcher) error

func atomicInc(i *int32) {
	atomic.AddInt32(i, 1)
}
func atomicGet(i *int32) int32 {
	return atomic.LoadInt32(i)
}

func AddressCreate(space *v1.MessagingAddress) interface{} {
	return &consolegraphql.AddressHolder{
		MessagingAddress: *space,
	}
}

func AddressUpdate(value *v1.MessagingAddress, existing interface{}) bool {
	ash := existing.(*consolegraphql.AddressHolder)
	if reflect.DeepEqual(ash.MessagingAddress, *value) {
		return false
	} else {
		value.DeepCopyInto(&ash.MessagingAddress)
		return true
	}
}

func MessagingProjectCreate(space *v1.MessagingProject) interface{} {
	return &consolegraphql.MessagingProjectHolder{
		MessagingProject: *space,
	}
}
func MessagingProjectUpdate(value *v1.MessagingProject, existing interface{}) bool {
	ash := existing.(*consolegraphql.MessagingProjectHolder)
	if reflect.DeepEqual(ash.MessagingProject, *value) {
		return false
	} else {
		value.DeepCopyInto(&ash.MessagingProject)
		return true
	}
}
