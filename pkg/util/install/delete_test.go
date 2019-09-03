/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"context"
	"testing"

	"k8s.io/apimachinery/pkg/api/errors"

	"sigs.k8s.io/controller-runtime/pkg/client"

	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	"github.com/google/uuid"

	"k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes/scheme"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

var p1 = v1.Pod{
	TypeMeta: metav1.TypeMeta{
		Kind:       "Pod",
		APIVersion: "v1",
	},
	ObjectMeta: metav1.ObjectMeta{
		Name:      "p1",
		Namespace: "ns",
		UID:       types.UID(uuid.New().String()),
	},
}

var s1 = v1.Secret{
	TypeMeta: metav1.TypeMeta{
		Kind:       "Secret",
		APIVersion: "v1",
	},
	ObjectMeta: metav1.ObjectMeta{
		Name:      "s1",
		Namespace: "ns",
		UID:       types.UID(uuid.New().String()),
	},
}
var c1 = v1.ConfigMap{
	TypeMeta: metav1.TypeMeta{
		Kind:       "ConfigMap",
		APIVersion: "v1",
	},
	ObjectMeta: metav1.ObjectMeta{
		Name:      "c1",
		Namespace: "ns",
		UID:       types.UID(uuid.New().String()),
	},
}
var c2 = v1.ConfigMap{
	TypeMeta: metav1.TypeMeta{
		Kind:       "ConfigMap",
		APIVersion: "v1",
	},
	ObjectMeta: metav1.ObjectMeta{
		Name:      "c2",
		Namespace: "ns",
		UID:       types.UID(uuid.New().String()),
	},
}

type IsOwnedResult struct {
	owned bool
	err   error
}

func toResult(owned bool, err error) IsOwnedResult {
	return IsOwnedResult{owned, err}
}

func assertOwnedResult(t *testing.T, expected bool, result IsOwnedResult) {
	if result.err != nil {
		t.Error("Should not return an error. Got: ", result.err)
	} else if expected != result.owned {
		t.Errorf("Predicate should return '%v', but did: %v", expected, result.owned)
	}
}

func TestIsOwnedBy1(t *testing.T) {

	if err := controllerutil.SetControllerReference(&s1, &c1, scheme.Scheme); err != nil {
		t.Fatal("Failed to set reference", err)
		return
	}

	assertOwnedResult(t, true, toResult(IsOwnedBy(&s1, &c1, true)))
	assertOwnedResult(t, false, toResult(IsOwnedBy(&s1, &c2, true)))
}

func TestIsOwnedBy2(t *testing.T) {

	if err := controllerutil.SetControllerReference(&s1, &c1, scheme.Scheme); err != nil {
		t.Fatal("Failed to set reference", err)
		return
	}

	assertOwnedResult(t, true, toResult(IsOwnedBy(&s1, &c1, false)))
	assertOwnedResult(t, false, toResult(IsOwnedBy(&s1, &c2, false)))

}

func TestIsOwnedBy3(t *testing.T) {

	ref1 := metav1.NewControllerRef(&c1, c1.GroupVersionKind())
	ref1.Controller = nil

	ref2 := metav1.NewControllerRef(&c2, c2.GroupVersionKind())
	ref2.Controller = nil

	s1.SetOwnerReferences([]metav1.OwnerReference{*ref1, *ref2})

	// controller = false

	assertOwnedResult(t, true, toResult(IsOwnedBy(&c1, &s1, false)))
	assertOwnedResult(t, true, toResult(IsOwnedBy(&c2, &s1, false)))

	// controller = true

	assertOwnedResult(t, false, toResult(IsOwnedBy(&c1, &s1, true)))
	assertOwnedResult(t, false, toResult(IsOwnedBy(&c2, &s1, true)))

}

func TestDeleteOwner(t *testing.T) {

	ctx := context.TODO()

	ref1 := metav1.NewControllerRef(&c1, c1.GroupVersionKind())
	ref1.Controller = nil

	ref2 := metav1.NewControllerRef(&c2, c2.GroupVersionKind())
	ref2.Controller = nil

	p1.SetOwnerReferences([]metav1.OwnerReference{*ref1, *ref2})

	c := fake.NewFakeClient(&c1, &c2, &p1)

	// initial test, must have two owners

	currentP1 := v1.Pod{}
	if err := c.Get(ctx, types.NamespacedName{Namespace: "ns", Name: "p1"}, &currentP1); err != nil {
		t.Fatalf("Should not return an error. Found: %v", err)
	}

	if currentP1.Name == "" {
		t.Fatal("Should have found P1")
	}

	if len(currentP1.OwnerReferences) != 2 {
		t.Fatalf("Should only have two owners. Found: %v", currentP1.OwnerReferences)
	}

	// now remove one owner

	n, err := BulkRemoveOwner(ctx, c, &c1, false, &v1.PodList{}, client.ListOptions{})

	if err != nil {
		t.Fatalf("Should not return an error. Found: %v", err)
	}

	if n != 1 {
		t.Errorf("Should report 1 item, found: %v", n)
	}

	// test again, must have only one owner

	currentP1 = v1.Pod{}
	if err := c.Get(ctx, types.NamespacedName{Namespace: "ns", Name: "p1"}, &currentP1); err != nil {
		t.Fatalf("Should not return an error. Found: %v", err)
	}

	if currentP1.Name == "" {
		t.Fatal("Should have found P1")
	}

	if len(currentP1.OwnerReferences) != 1 {
		t.Errorf("Should only have one owner. Found: %v", currentP1.OwnerReferences)
	}

	// remove last owner

	n, err = BulkRemoveOwner(ctx, c, &c2, false, &v1.PodList{}, client.ListOptions{})

	if err != nil {
		t.Fatalf("Should not return an error. Found: %v", err)
	}

	if n != 1 {
		t.Errorf("Should report 1 item, found: %v", n)
	}

	// object must be deleted

	currentP1 = v1.Pod{}
	if err := c.Get(ctx, types.NamespacedName{Namespace: "ns", Name: "p1"}, &currentP1); err != nil {
		if !errors.IsNotFound(err) {
			t.Fatalf("Must be a not-found error. Found: %v", err)
		}

	} else {
		t.Error("Should return a not found error")
	}

}
