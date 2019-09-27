/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cache

import (
	"fmt"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/types"
	"strings"
	"testing"
)

const altHierarchy = "altHierarchy"

type CacheObj struct {
	metav1.TypeMeta
	metav1.ObjectMeta
	Spec CacheObjSpec
}

type CacheObjSpec struct {
	Attr string
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

func LinkIndexCreator2(o runtime.Object) (string, error) {
	co, ok := o.(*CacheObj)
	if !ok {
		return "", fmt.Errorf("unexpected type")
	}

	return co.Kind + "/" + co.Namespace + "/" + co.Spec.Attr, nil
}

func newTestCache(t *testing.T, alternativeHierarchy bool) Cache {
	cache := &MemdbCache{}

	var err error
	specifiers := make([]IndexSpecifier, 0)
	specifiers = append(specifiers,
		IndexSpecifier{
			Name:    "id",
			Indexer: &UidIndex{},
		},
		IndexSpecifier{
			Name: "hierarchy",
			Indexer: &HierarchyIndex{
				IndexCreators: map[string]HierarchicalIndexCreator{
					"CacheObj": ObjectIndexCreator,
				},
			},
		})
	if alternativeHierarchy {
		specifiers = append(specifiers, IndexSpecifier{
			Name: altHierarchy,
			Indexer: &HierarchyIndex{
				IndexCreators: map[string]HierarchicalIndexCreator{
					"CacheObj": LinkIndexCreator2,
				},
			},
		})
	}
	err = cache.Init(specifiers...)
	assert.NoError(t, err)

	return cache
}

func TestInsert(t *testing.T) {
	c := newTestCache(t, false)

	obj := createObj("ns1", "ob1")
	err := c.Add(obj)
	assert.NoError(t, err, "failed to insert object")

	retrieved, err := c.Get("id", string(obj.UID), nil)
	assert.NoError(t, err, "failed to retrieve object")

	assert.Equal(t, 1, len(retrieved), "Unexpected object")
	assert.Equal(t, obj, retrieved[0], "Unexpected object")
}
func TestMemdbCache_Delete(t *testing.T) {
	c := newTestCache(t, false)

	obj := createObj("ns1", "ob1")
	err := c.Add(obj)
	assert.NoError(t, err, "failed to insert object")

	retrieved, err := c.Get("id", string(obj.UID), nil)
	assert.NoError(t, err, "failed to query object")
	assert.Equal(t, 1, len(retrieved), "failed to retrieve object")

	err = c.Delete(obj)
	assert.NoError(t, err, "failed to delete object")

	reretrieved, err := c.Get("id", string(obj.UID), nil)
	assert.NoError(t, err, "failed to query object")
	assert.Equal(t, 0, len(reretrieved), "unexpectedly retrieved object after deletion")
}

func TestMemdbCache_DeleteByPrefix(t *testing.T) {
	c := newTestCache(t, true)

	doSubsetTest := func(idx, idxpath string, expected int) {

		obj1 := createObj("ns1", "aab")
		obj2 := createObj("ns1", "aa")
		obj3 := createObj("ns1", "a")
		err := c.Add(obj1, obj2, obj3)
		assert.NoError(t, err, "failed to insert object")

		err = c.DeleteByPrefix(idx, idxpath)
		assert.NoError(t, err, "failed to delete object")

		subset, err := c.Get(idx, "CacheObject/", nil)
		assert.NoError(t, err, "failed to retrieve objects")

		actual := len(subset)
		assert.Equalf(t, expected, actual, "Unexpected number of links after delete of prefix %s", idxpath)
	}

	doSubsetTest("hierarchy", "CacheObject", 0)
	doSubsetTest("hierarchy", "CacheObject/ns1/a", 0)
	doSubsetTest("hierarchy", "CacheObject/ns1/aa", 1)
}

func TestMemdbCache_Get(t *testing.T) {
	c := newTestCache(t, false)

	doSubsetTest := func(idxpath string, expected int) {
		subset, err := c.Get("hierarchy", idxpath, nil)
		assert.NoError(t, err, "failed to retrieve objects")

		actual := len(subset)
		assert.Equalf(t, expected, actual, "Unexpected number of objects retrieved for index prefix %s", idxpath)
	}

	obj1 := createObj("ns1", "a")
	obj2 := createObj("ns1", "ab")
	obj3 := createObj("ns1", "abc")
	obj4 := createObj("ns2", "abc")
	err := c.Add(obj1, obj2, obj3, obj4)
	assert.NoError(t, err, "failed to insert objects")

	doSubsetTest("CacheObject", 4)
	doSubsetTest("CacheObject/ns1/ab", 2)
	doSubsetTest("CacheObject/ns1/a", 3)
	doSubsetTest("CacheObject/ns2", 1)
	doSubsetTest("CacheObject/ns", 4)
	doSubsetTest("CacheObject/wibb", 0)
}

func TestMemdbCache_FilteredGet(t *testing.T) {
	c := newTestCache(t, false)

	obj1 := createObj("ns1", "a")
	obj2 := createObj("ns1", "ab")
	err := c.Add(obj1, obj2)
	assert.NoError(t, err, "failed to insert objects")

	subset, err := c.Get("hierarchy", "CacheObject", func(object interface{}) (bool, bool, error) {
		return object.(*CacheObj).Name == obj1.Name, true, nil
	})
	assert.NoError(t, err, "failed to retrieve objects")

	expected := 1
	actual := len(subset)
	assert.Equal(t, expected, actual, "Unexpected number of objects retrieved for filter")

	actualObj := subset[0]
	assert.Equal(t, obj1, actualObj, "Unexpected object")
}

func TestMemdbCache_FilteredGetStop(t *testing.T) {
	c := newTestCache(t, false)

	obj1 := createObj("ns1", "a")
	obj2 := createObj("ns1", "ab")
	obj3 := createObj("ns1", "abc")
	err := c.Add(obj1, obj2, obj3)
	assert.NoError(t, err, "failed to insert objects")

	subset, err := c.Get("hierarchy", "CacheObject", func(object interface{}) (bool, bool, error) {
		name := object.(*CacheObj).Name
		return strings.HasPrefix(obj1.Name, "a"), name == "a", nil
	})
	assert.NoError(t, err, "failed to retrieve objects")

	expected := 2
	actual := len(subset)
	assert.Equal(t, expected, actual, "Unexpected number of objects retrieved for filter")

	actualObj := subset[1]
	assert.Equal(t, obj2, actualObj, "Unexpected object")
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
		Spec: CacheObjSpec{},
	}
}

func TestMemdbCache_Get_AltIndex(t *testing.T) {
	c := newTestCache(t, true)

	doSubsetTest := func(idx, idxpath string, expected int) {
		subset, err := c.Get(idx, idxpath, nil)
		assert.NoError(t, err, "failed to retrieve objects")

		actual := len(subset)
		assert.Equalf(t, expected, actual, "Unexpected number of objects retrieved for index prefix %s", idxpath)
	}

	obj1 := createObjWithAttr("ns1", "foo1", "bar1")
	obj2 := createObjWithAttr("ns1", "foo2", "bar2")
	err := c.Add(obj1, obj2)
	assert.NoError(t, err, "failed to insert objects")

	doSubsetTest("hierarchy", "CacheObject", 2)
	doSubsetTest("hierarchy", "CacheObject/ns1/foo", 2)
	doSubsetTest("hierarchy", "CacheObject/ns1/foo1", 1)
	doSubsetTest(altHierarchy, "CacheObject/ns1/bar", 2)
	doSubsetTest(altHierarchy, "CacheObject/ns1/bar1", 1)
}

func TestMemdbCache_DeleteByPrefix_AltIndex(t *testing.T) {
	c := newTestCache(t, true)

	doSubsetTest := func(idx, idxpath string, expected int) {

		obj1 := createObjWithAttr("ns1", "aab", "bbc")
		obj2 := createObjWithAttr("ns1", "aa", "bb")
		obj3 := createObjWithAttr("ns1", "a", "b")
		err := c.Add(obj1, obj2, obj3)
		assert.NoError(t, err, "failed to insert objects")

		err = c.DeleteByPrefix(idx, idxpath)
		assert.NoError(t, err, "failed to delete links")

		subset, err := c.Get(idx, "CacheObject/", nil)
		assert.NoError(t, err, "failed to retrieve links")

		actual := len(subset)
		assert.Equalf(t, expected, actual, "Unexpected number of links after delete of prefix %s", idxpath)
	}

	doSubsetTest("hierarchy", "CacheObject/ns1/aa", 1)
	doSubsetTest(altHierarchy, "CacheObject/ns1/bbc", 2)
	// Wrong index - won't delete anything
	doSubsetTest("hierarchy", "CacheObject/ns1/bbc", 3)
}

func TestDump(t *testing.T) {
	c := newTestCache(t, false)

	obj := createObj("ns1", "ob1")
	err := c.Add(obj)
	assert.NoError(t, err, "failed to insert object")

	err = c.Dump()
	assert.NoError(t, err, "failed to dump database")
}

func createObjWithAttr(namespace, name, attr string) *CacheObj {
	obj := createObj(namespace, name)
	obj.Spec.Attr = attr
	return obj
}
