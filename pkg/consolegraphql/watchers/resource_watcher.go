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

import (
	"fmt"
	adminv1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"strings"
)

type ResourceWatcher interface {
	//NewClientForConfig(config *rest.Config) (interface{}, error)
	Watch() error
	AwaitWatching()
	Shutdown()
}

type WatcherOption func(watcher ResourceWatcher) error


func NamespaceIndexCreator(o runtime.Object) (string, error) {
	ns, ok := o.(*v1.Namespace)
	if !ok {
		return "", fmt.Errorf("unexpected type %T", o)
	}

	return ns.Kind + "/" + ns.Name, nil
}

func AddressSpaceIndexCreator(o runtime.Object) (string, error) {
	as, ok := o.(*consolegraphql.AddressSpaceHolder)
	if !ok {
		return "", fmt.Errorf("unexpected type")
	}

	return as.Kind + "/" + as.Namespace + "/" + as.Name, nil
}

func AddressIndexCreator(o runtime.Object) (string, error) {
	a, ok := o.(*consolegraphql.AddressHolder)
	if !ok {
		return "", fmt.Errorf("unexpected type")
	}

	i := strings.Index(a.Name, ".")
	if i < 0 {
		return "", fmt.Errorf("unexpected name formation '%s', expected dot separator", a.Name)
	}
	addressSpaceName := a.Name[:i]

	return a.Kind + "/" + a.Namespace + "/" + addressSpaceName + "/" + a.Name, nil
}

func AddressPlanIndexCreator(o runtime.Object) (string, error) {
	ap, ok := o.(*v1beta2.AddressPlan)
	if !ok {
		return "", fmt.Errorf("unexpected type")
	}

	return ap.Kind + "/" + ap.Namespace + "/" + ap.Name, nil
}

func AddressSpacePlanIndexCreator(o runtime.Object) (string, error) {
	asp, ok := o.(*v1beta2.AddressSpacePlan)
	if !ok {
		return "", fmt.Errorf("unexpected type")
	}

	return asp.Kind + "/" + asp.Namespace + "/" + asp.Name, nil
}

func AuthenticationServiceIndexCreator(o runtime.Object) (string, error) {
       as, ok := o.(*adminv1.AuthenticationService)
       if !ok {
               return "", fmt.Errorf("unexpected type")
       }

       return as.Kind + "/" + as.Namespace + "/" + as.Name, nil
}

func AddressSpaceSchemaIndexCreator(o runtime.Object) (string, error) {
       ass, ok := o.(*v1beta1.AddressSpaceSchema)
       if !ok {
               return "", fmt.Errorf("unexpected type")
       }

       return ass.Kind + "/" + ass.Namespace + "/" + ass.Name, nil
}

func ConnectionIndexCreator(o runtime.Object) (string, error) {
	con, ok := o.(*consolegraphql.Connection)
	if !ok {
		return "", fmt.Errorf("unexpected type")
	}

	return con.Kind + "/" + con.Namespace + "/" + con.Spec.AddressSpace + "/" + con.Name, nil
}

func ConnectionLinkIndexCreator(o runtime.Object) (string, error) {
	asp, ok := o.(*consolegraphql.Link)
	if !ok {
		return "", fmt.Errorf("unexpected type")
	}

	return asp.Kind + "/" + asp.Namespace + "/" + asp.Spec.AddressSpace + "/" + asp.Spec.Connection + "/" + asp.ObjectMeta.Name, nil
}

func AddressLinkIndexCreator(o runtime.Object) (string, error) {
	asp, ok := o.(*consolegraphql.Link)
	if !ok {
		return "", fmt.Errorf("unexpected type")
	}

	return asp.Kind + "/" + asp.Namespace + "/" + asp.Spec.AddressSpace + "/" + asp.Spec.Address + "/" + asp.ObjectMeta.Name, nil
}
