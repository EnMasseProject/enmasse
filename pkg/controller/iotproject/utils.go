/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"fmt"
	"strings"

	"sigs.k8s.io/controller-runtime/pkg/client/apiutil"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"

	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

func splitUserName(name string) (string, string, error) {
	tokens := strings.Split(name, ".")

	if len(tokens) != 2 {
		return "", "", fmt.Errorf("username in wrong format, must be <addressspace>.<name>: %v", name)
	}

	return tokens[0], tokens[1], nil
}

// Ensure that controller owner is set
// As there may only be one, we only to this when the creation timestamp is zero
func (r *ReconcileIoTProject) ensureControllerOwnerIsSet(owner, object v1.Object) error {

	if isNewObject(object) {
		err := controllerutil.SetControllerReference(owner, object, r.scheme)
		if err != nil {
			return err
		}
	}

	return nil
}

func (r *ReconcileIoTProject) ensureOwnerIsSet(owner, object v1.Object) error {

	ro, ok := owner.(runtime.Object)
	if !ok {
		return fmt.Errorf("is not a %T a runtime.Object, cannot call ensureOwnerIsSet", owner)
	}

	gvk, err := apiutil.GVKForObject(ro, r.scheme)
	if err != nil {
		return err
	}

	// create our ref
	newref := *NewOwnerRef(owner, gvk)

	// get existing refs
	refs := object.GetOwnerReferences()

	found := false
	for _, ref := range refs {
		if isSameRef(ref, newref) {
			found = true
		}
	}

	// did we find it?
	if !found {
		// no! so append
		refs = append(refs, newref)
	}

	// set the new result
	object.SetOwnerReferences(refs)

	return nil
}

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

func isNewObject(object v1.Object) bool {
	ts := object.GetCreationTimestamp()
	return ts.IsZero()
}

func isSameRef(ref1, ref2 v1.OwnerReference) bool {

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
