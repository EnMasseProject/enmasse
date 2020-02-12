/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

//go:generate go run ../../../hack/generate_resource_watchers.go AddressSpace EnmasseV1beta1Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1 github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1
//go:generate go run ../../../hack/generate_resource_watchers.go Address EnmasseV1beta1Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1 github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1

//go:generate go run ../../../hack/generate_resource_watchers.go --watchAll=false AddressPlan AdminV1beta2Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2 github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2
//go:generate go run ../../../hack/generate_resource_watchers.go --watchAll=false AddressSpacePlan AdminV1beta2Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2 github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2
//go:generate go run ../../../hack/generate_resource_watchers.go --watchAll=false AuthenticationService AdminV1beta1Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta1 github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1

//go:generate go run ../../../hack/generate_resource_watchers.go --scope clusterwide Namespace CoreV1Interface k8s.io/client-go/kubernetes/typed/core/v1 k8s.io/api/core/v1
//go:generate go run ../../../hack/generate_resource_watchers.go --scope clusterwide AddressSpaceSchema EnmasseV1beta1Interface github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1 github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1

package watchers

type ResourceWatcher interface {
	//NewClientForConfig(config *rest.Config) (interface{}, error)
	Watch() error
	AwaitWatching()
	Shutdown()
}

type WatcherOption func(watcher ResourceWatcher) error
