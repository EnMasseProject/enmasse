/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cache

import (
	"fmt"
	"github.com/google/uuid"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/types"
	"reflect"
	"testing"
)

type CacheObj struct {
	metav1.TypeMeta
	metav1.ObjectMeta
}

func (c CacheObj) SetGroupVersionKind(kind schema.GroupVersionKind) {
	panic("unused")
}

func (c CacheObj) GroupVersionKind() schema.GroupVersionKind {
	return schema.GroupVersionKind{
		Group:   "test.enmasse.io",
		Version: "beta1",
		Kind:    "CacheObj",
	}
}

func (c CacheObj) GetObjectKind() schema.ObjectKind {
	return c
}

func (c CacheObj) DeepCopyObject() runtime.Object {
	return CacheObj{
		ObjectMeta: metav1.ObjectMeta{
			Name: c.Name,
			UID:  c.UID,
		},
	}
}

func ObjectIndexCreator(o runtime.Object) (string, error) {
	co, ok := o.(*CacheObj)
	if !ok {
		return "", fmt.Errorf("unexpected type")
	}

	return co.Kind + "/" + co.Namespace + "/" + co.Name, nil
}

func newTestCache(t *testing.T) Cache {
	cache := &MemdbCache{}
	err := cache.Init()
	if err != nil {
		t.Fatal("failed to create test resolver")
	}
	cache.RegisterIndexCreator("CacheObj", ObjectIndexCreator)

	return cache
}

func TestInsert(t *testing.T) {
	c := newTestCache(t)

	obj := createObj("ns1", "ob1")
	err := c.Add(obj)
	if err != nil {
		t.Fatal("failed to insert object", err)
	}

	retrieved, err := c.GetByUID(obj.UID)
	if err != nil {
		t.Fatal("failed to retrieve object", err)
	}

	if !reflect.DeepEqual(obj, retrieved) {
		t.Fatalf("Unexpected connection, expected %+v, actual %+v", obj, retrieved)
	}
}

func TestDelete(t *testing.T) {
	c := newTestCache(t)

	obj := createObj("ns1", "ob1")
	err := c.Add(obj)
	if err != nil {
		t.Fatal("failed to insert object", err)
	}

	retrieved, err := c.GetByUID(obj.UID)
	if err != nil || retrieved == nil {
		t.Fatal("failed to retrieve object", err)
	}

	err = c.Delete(obj)
	if err != nil {
		t.Fatal("failed to delete object", err)
	}

	reretrieved, err := c.GetByUID(obj.UID)
	if reretrieved != nil || err != nil {
		t.Fatal("unexpectedly retrieved object after deletion", err)
	}

	if !reflect.DeepEqual(obj, retrieved) {
		t.Fatalf("Unexpected connection, expected %+v, actual %+v", obj, retrieved)
	}
}

func TestGet(t *testing.T) {
	c := newTestCache(t)

	doSubsetTest := func(idxpath string, expected int) {
		subset, err := c.Get(idxpath, nil)
		if err != nil {
			t.Fatal("failed to retrieve objects", err)
		}

		actual := len(subset)
		if actual != expected {
			t.Fatalf("Unexpected number of objects retrieved for index prefix %s, expected %d actual %d", idxpath, expected, actual)
		}
	}

	obj1 := createObj("ns1", "a")
	obj2 := createObj("ns1", "ab")
	obj3 := createObj("ns1", "abc")
	obj4 := createObj("ns2", "abc")
	err := c.Add(obj1, obj2, obj3, obj4)
	if err != nil {
		t.Fatal("failed to insert object", err)
	}

	doSubsetTest("CacheObject", 4)
	doSubsetTest("CacheObject/ns1/ab", 2)
	doSubsetTest("CacheObject/ns1/a", 3)
	doSubsetTest("CacheObject/ns2", 1)
	doSubsetTest("CacheObject/ns", 4)
	doSubsetTest("CacheObject/wibb", 0)
}

func TestFilteredGet(t *testing.T) {
	c := newTestCache(t)

	obj1 := createObj("ns1", "a")
	obj2 := createObj("ns1", "ab")
	err := c.Add(obj1, obj2)
	if err != nil {
		t.Fatal("failed to insert object", err)
	}

	subset, err := c.Get("CacheObject", func(object runtime.Object) (b bool, e error) {
		return object.(*CacheObj).Name == obj1.Name, nil
	})
	if err != nil {
		t.Fatal("failed to retrieve objects", err)
	}

	expected := 1
	actual := len(subset)
	if actual != expected {
		t.Fatalf("Unexpected number of objects retrieved for filter, expected %d actual %d", expected, actual)
	}

	actualObj := subset[0]
	if !reflect.DeepEqual(obj1, actualObj) {
		t.Fatalf("Unexpected connection, expected %+v, actual %+v", obj1, actualObj)
	}
}

func TestGetMap(t *testing.T) {
	c := newTestCache(t)

	obj1 := createObj("ns1", "a")
	obj2 := createObj("ns1", "b")
	err := c.Add(obj1, obj2)
	if err != nil {
		t.Fatal("failed to insert object", err)
	}

	mapped, err := c.GetMap("CacheObject")
	if err != nil {
		t.Fatal("failed to retrieve objects", err)
	}

	expected := 2
	actual := len(mapped)
	if actual != expected {
		t.Fatalf("Unexpected number of objects retrieved, expected %d actual %d", expected, actual)
	}

	actualObj := mapped[obj1.UID]
	if !reflect.DeepEqual(obj1, actualObj) {
		t.Fatalf("Unexpected connection, expected %+v, actual %+v", obj1, actualObj)
	}
}

func createObj(namespace string, name string) *CacheObj {
	return &CacheObj{
		TypeMeta: metav1.TypeMeta{
			Kind: "CacheObject",
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: namespace,
			Name:      name,
			UID:       types.UID(uuid.New().String()),
		},
	}
}
