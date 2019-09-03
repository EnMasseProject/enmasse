/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package finalizer

import (
	"context"
	"fmt"

	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

type DeconstructorContext struct {
	Context context.Context
	Client  client.Client
	Object  runtime.Object
}

type Finalizer struct {
	// the name of the finalizer
	Name string

	// The deconstructor function, which will be called when the finalizer
	// is still present on the object, and the deletion timestamp is set.
	// When the deconstructor is finished, or had nothing to do, it must return
	// a nil error, and a result which does not request a requeue.
	//
	// If the error is non-nil, or a requeue was requested, the finalizer will
	// *not* be removed from the list.
	Deconstruct func(ctx DeconstructorContext) (reconcile.Result, error)
}

func hasFinalizer(name string, list []string) bool {
	for _, e := range list {
		if name == e {
			return true
		}
	}
	return false
}

func removeFinalizer(name string, list []string) []string {

	if list == nil {
		return list
	}

	// removing an entry from an array in go is tricky ...

	for i := len(list) - 1; i >= 0; i-- {
		v := list[i]
		if v == name {
			list = append(
				list[:i],
				list[i+1:]...,
			)
		}
	}

	return list

}

// The function will process finalizers of the object.
//
// If the deletion timestamp is nil, then it will add all names of all missing finalizers, and update the object if
// this caused a change.
//
// If the deletion timestamp is non-nil, it will iterate over the finalizers, and call the destructor for the first
// finalizer it still finds in the list (in any order). If the deconstructor returns as "finished", then will remove
// the finalizer from the list, update the object, and request to be re-queued.
func ProcessFinalizers(ctx context.Context, client client.Client, obj runtime.Object, finalizers []Finalizer) (reconcile.Result, error) {

	object, ok := obj.(v1.Object)
	if !ok {
		return reconcile.Result{}, fmt.Errorf("unable to process an object, which is no a v1.Object")
	}

	if object.GetDeletionTimestamp() == nil {

		// the object it _not_ scheduled for deletion

		changed := false
		current := object.GetFinalizers()

		// iterate over the list of expected finalizers

		for _, f := range finalizers {
			if !hasFinalizer(f.Name, current) {
				// if the finalizer is missing, add it
				changed = true
				current = append(current, f.Name)
			}
		}

		if changed {
			// the list of finalizers has changed, update and return
			object.SetFinalizers(current)
			return reconcile.Result{Requeue: true}, client.Update(ctx, obj)
		}

	} else {

		// the object _is_ scheduled for deletion

		current := object.GetFinalizers()

		for _, f := range finalizers {

			if hasFinalizer(f.Name, current) {

				// if the finalizer is still present ...

				// ... run the deconstructor

				var result = reconcile.Result{}
				var err error = nil
				if f.Deconstruct != nil {
					result, err = f.Deconstruct(DeconstructorContext{
						Context: ctx,
						Client:  client,
						Object:  obj,
					})
				}

				// process the result

				if err != nil {
					return reconcile.Result{}, err
				}

				if !result.Requeue && result.RequeueAfter <= 0 {

					// we had no error, and do not need to check again, so remove the finalizer

					// remove it from the list, and set the update
					c := current
					object.SetFinalizers(removeFinalizer(f.Name, c))
					// persist, and re-schedule for the next finalizer
					return reconcile.Result{Requeue: true}, client.Update(ctx, obj)

				} else {

					// return the re-queue request
					return result, nil

				}

			}
		}

	}

	// nothing had to be done

	return reconcile.Result{}, nil

}
