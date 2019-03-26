/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/util"

	"sigs.k8s.io/controller-runtime/pkg/client/apiutil"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"

	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
)

// Ensure that controller owner is set
// As there may only be one, we only to this when the creation timestamp is zero
func (r *ReconcileIoTProject) ensureControllerOwnerIsSet(owner, object v1.Object) error {

	if util.IsNewObject(object) {
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
	newref := *util.NewOwnerRef(owner, gvk)

	// get existing refs
	refs := object.GetOwnerReferences()

	found := false
	for _, ref := range refs {
		if util.IsSameRef(ref, newref) {
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

func StringOrDefault(value string, defaultValue string) string {
	if value == "" {
		return defaultValue
	} else {
		return value
	}
}
