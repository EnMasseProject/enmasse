/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1

import (
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

const GroupVersion = "v1"

var SchemeGroupVersion = schema.GroupVersion{Group: enmasse.GroupName, Version: GroupVersion}

func Kind(kind string) schema.GroupKind {
	return SchemeGroupVersion.WithKind(kind).GroupKind()
}

func Resource(resource string) schema.GroupResource {
	return SchemeGroupVersion.WithResource(resource).GroupResource()
}

var (
	SchemeBuilder = runtime.NewSchemeBuilder(addKnownTypes)
	AddToScheme   = SchemeBuilder.AddToScheme
)

func addKnownTypes(scheme *runtime.Scheme) error {
	scheme.AddKnownTypes(SchemeGroupVersion,
		&MessagingInfrastructure{},
		&MessagingInfrastructureList{},
		&MessagingTenant{},
		&MessagingTenantList{},
		&MessagingAddress{},
		&MessagingAddressList{},
		&MessagingEndpoint{},
		&MessagingEndpointList{},
		&MessagingPlan{},
		&MessagingPlanList{},
		&MessagingAddressPlan{},
		&MessagingAddressPlanList{},
	)

	metav1.AddToGroupVersion(scheme, SchemeGroupVersion)
	return nil
}
