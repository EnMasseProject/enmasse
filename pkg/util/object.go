/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

func NewOwnerRef(owner v1.Object, gvk schema.GroupVersionKind) *v1.OwnerReference {
	blockOwnerDeletion := false
	isController := false
	return &v1.OwnerReference{
		APIVersion:         gvk.GroupVersion().String(),
		Kind:               gvk.Kind,
		Name:               owner.GetName(),
		UID:                owner.GetUID(),
		BlockOwnerDeletion: &blockOwnerDeletion,
		Controller:         &isController,
	}
}

func IsNewObject(object v1.Object) bool {
	ts := object.GetCreationTimestamp()
	return ts.IsZero()
}

func IsSameRef(ref1, ref2 v1.OwnerReference) bool {

	gv1, err := schema.ParseGroupVersion(ref1.APIVersion)
	if err != nil {
		return false
	}

	gv2, err := schema.ParseGroupVersion(ref2.APIVersion)
	if err != nil {
		return false
	}

	return ref1.Kind == ref2.Kind &&
		gv1.Group == gv2.Group &&
		ref1.Name == ref2.Name &&
		ref1.UID == ref2.UID
}
