/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"testing"

	"github.com/google/uuid"

	"k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes/scheme"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

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

func TestIsOwnedBy1(t *testing.T) {

	if err := controllerutil.SetControllerReference(&s1, &c1, scheme.Scheme); err != nil {
		t.Fatal("Failed to set reference", err)
		return
	}

	if !IsOwnedByPredicate(&s1, true)(&c1) {
		t.Error("Predicate should return 'true'")
	}
	if IsOwnedByPredicate(&s1, true)(&c2) {
		t.Error("Predicate should return 'false'")
	}
}

func TestIsOwnedBy2(t *testing.T) {

	if err := controllerutil.SetControllerReference(&s1, &c1, scheme.Scheme); err != nil {
		t.Fatal("Failed to set reference", err)
		return
	}

	if !IsOwnedByPredicate(&s1, false)(&c1) {
		t.Error("Predicate should return 'true'")
	}
	if IsOwnedByPredicate(&s1, false)(&c2) {
		t.Error("Predicate should return 'false'")
	}
}

func TestIsOwnedBy3(t *testing.T) {

	ref1 := metav1.NewControllerRef(&c1, c1.GroupVersionKind())
	ref1.Controller = nil

	ref2 := metav1.NewControllerRef(&c2, c2.GroupVersionKind())
	ref2.Controller = nil

	s1.SetOwnerReferences([]metav1.OwnerReference{*ref1, *ref2})

	// controller = false

	if !IsOwnedByPredicate(&c1, false)(&s1) {
		t.Error("Predicate should return 'true'")
	}
	if !IsOwnedByPredicate(&c2, false)(&s1) {
		t.Error("Predicate should return 'true'")
	}

	// controller = true

	if IsOwnedByPredicate(&c1, true)(&s1) {
		t.Error("Predicate should return 'false'")
	}
	if IsOwnedByPredicate(&c2, true)(&s1) {
		t.Error("Predicate should return 'false'")
	}
}
