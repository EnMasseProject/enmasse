/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

// Code generated by informer-gen. DO NOT EDIT.

package externalversions

import (
	"fmt"

	v1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	schema "k8s.io/apimachinery/pkg/runtime/schema"
	cache "k8s.io/client-go/tools/cache"
)

// GenericInformer is type of SharedIndexInformer which will locate and delegate to other
// sharedInformers based on type
type GenericInformer interface {
	Informer() cache.SharedIndexInformer
	Lister() cache.GenericLister
}

type genericInformer struct {
	informer cache.SharedIndexInformer
	resource schema.GroupResource
}

// Informer returns the SharedIndexInformer.
func (f *genericInformer) Informer() cache.SharedIndexInformer {
	return f.informer
}

// Lister returns the GenericLister.
func (f *genericInformer) Lister() cache.GenericLister {
	return cache.NewGenericLister(f.Informer().GetIndexer(), f.resource)
}

// ForResource gives generic access to a shared informer of the matching type
// TODO extend this to unknown resources with a client pool
func (f *sharedInformerFactory) ForResource(resource schema.GroupVersionResource) (GenericInformer, error) {
	switch resource {
	// Group=admin.enmasse.io, Version=v1beta1
	case v1beta1.SchemeGroupVersion.WithResource("authenticationservices"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Admin().V1beta1().AuthenticationServices().Informer()}, nil
	case v1beta1.SchemeGroupVersion.WithResource("consoleservices"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Admin().V1beta1().ConsoleServices().Informer()}, nil

		// Group=admin.enmasse.io, Version=v1beta2
	case v1beta2.SchemeGroupVersion.WithResource("addressplans"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Admin().V1beta2().AddressPlans().Informer()}, nil
	case v1beta2.SchemeGroupVersion.WithResource("addressspaceplans"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Admin().V1beta2().AddressSpacePlans().Informer()}, nil

		// Group=enmasse.io, Version=v1
	case v1.SchemeGroupVersion.WithResource("messagingaddresses"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1().MessagingAddresses().Informer()}, nil
	case v1.SchemeGroupVersion.WithResource("messagingaddressplans"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1().MessagingAddressPlans().Informer()}, nil
	case v1.SchemeGroupVersion.WithResource("messagingendpoints"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1().MessagingEndpoints().Informer()}, nil
	case v1.SchemeGroupVersion.WithResource("messaginginfrastructures"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1().MessagingInfrastructures().Informer()}, nil
	case v1.SchemeGroupVersion.WithResource("messagingplans"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1().MessagingPlans().Informer()}, nil
	case v1.SchemeGroupVersion.WithResource("messagingprojects"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1().MessagingProjects().Informer()}, nil

		// Group=enmasse.io, Version=v1beta1
	case enmassev1beta1.SchemeGroupVersion.WithResource("addresses"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1beta1().Addresses().Informer()}, nil
	case enmassev1beta1.SchemeGroupVersion.WithResource("addressspaces"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1beta1().AddressSpaces().Informer()}, nil
	case enmassev1beta1.SchemeGroupVersion.WithResource("addressspaceschemas"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1beta1().AddressSpaceSchemas().Informer()}, nil
	case enmassev1beta1.SchemeGroupVersion.WithResource("authenticationservices"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Enmasse().V1beta1().AuthenticationServices().Informer()}, nil

		// Group=iot.enmasse.io, Version=v1
	case iotv1.SchemeGroupVersion.WithResource("iotconfigs"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Iot().V1().IoTConfigs().Informer()}, nil
	case iotv1.SchemeGroupVersion.WithResource("iottenants"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.Iot().V1().IoTTenants().Informer()}, nil

		// Group=user.enmasse.io, Version=v1beta1
	case userv1beta1.SchemeGroupVersion.WithResource("messagingusers"):
		return &genericInformer{resource: resource.GroupResource(), informer: f.User().V1beta1().MessagingUsers().Informer()}, nil

	}

	return nil, fmt.Errorf("no informer found for %v", resource)
}
