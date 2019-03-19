/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"context"
	"fmt"
	"reflect"

	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"

	"k8s.io/apimachinery/pkg/apis/meta/v1"

	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/selection"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type DeletePredicate func(interface{}) bool

// Function to check if an object is owned by another object
func OwnedBy(obj v1.Object, owner runtime.Object, controller bool) bool {

	ownerKind := owner.GetObjectKind()
	ownerKindName := ownerKind.GroupVersionKind().Kind
	ownerApiVersion := ownerKind.GroupVersionKind().GroupVersion().String()

	// get name

	oma, ok := owner.(v1.ObjectMetaAccessor)
	if !ok {
		return false
	}
	meta := oma.GetObjectMeta()

	// get references

	refs := obj.GetOwnerReferences()

	for _, r := range refs {

		if controller {
			if r.Controller == nil || !*r.Controller {
				continue
			}
		}

		if r.Kind != ownerKindName {
			continue
		}

		if r.APIVersion != ownerApiVersion {
			continue
		}

		if r.Name != meta.GetName() {
			continue
		}

		if r.UID != meta.GetUID() {
			continue
		}

		return true

	}

	return false

}

// Create a new predicate, calling OwnedBy
func IsOwnedByPredicate(owner runtime.Object, controller bool) DeletePredicate {

	return func(obj interface{}) bool {
		uo, ok := obj.(unstructured.Unstructured)
		if ok {
			return OwnedBy(&uo, owner, controller)
		}

		oma, ok := obj.(v1.ObjectMetaAccessor)
		if ok {
			return OwnedBy(oma.GetObjectMeta(), owner, controller)
		}

		return false
	}
}

// Bulk delete
// The "obj" provided must by a Kubernetes List type, having an "Items" field
func BulkDelete(ctx context.Context, client client.Client, obj runtime.Object, opts client.ListOptions, predicate DeletePredicate) (int, error) {

	if err := client.List(ctx, &opts, obj); err != nil {
		log.Error(err, "Failed to list items to delete")
		return -1, err
	}

	val := reflect.ValueOf(obj).Elem()
	items := val.FieldByName("Items")

	if items.Kind() != reflect.Slice {
		return -1, fmt.Errorf("object does not have an array named 'Items'")
	}

	l := items.Len()
	n := 0
	for i := 0; i < l; i++ {
		item := items.Index(i)

		obj := item.Interface()

		if predicate != nil && !predicate(obj) {
			continue
		}

		n++

		o, ok := obj.(runtime.Object)
		if ok {
			if err := client.Delete(ctx, o); err != nil {
				return -1, err
			}
		}
		o2, ok := obj.(unstructured.Unstructured)
		if ok {
			if err := client.Delete(ctx, &o2); err != nil {
				return -1, err
			}
		}
	}

	return n, nil
}

// Bulk delete objects by a LabelSelector, defined as a map
// The "obj" provided must by a Kubernetes List type, having an "Items" field
func BulkDeleteByLabelMap(ctx context.Context, c client.Client, obj runtime.Object, namespace string, l map[string]string, predicate DeletePredicate) (int, error) {

	ls := labels.NewSelector()

	for k, v := range l {
		r, err := labels.NewRequirement(k, selection.Equals, []string{v})
		if err != nil {
			return -1, err
		}
		ls = ls.Add(*r)
	}

	return BulkDelete(ctx, c, obj, client.ListOptions{
		Namespace:     namespace,
		LabelSelector: ls,
	}, predicate)

}
