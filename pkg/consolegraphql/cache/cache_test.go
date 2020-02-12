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
	"k8s.io/apimachinery/pkg/types"
	"strings"
	"testing"
)

const idIndex = "id"
const altIndex = "altIndex"

type CacheObj struct {
	metav1.TypeMeta
	metav1.ObjectMeta
	Spec CacheObjSpec
}

type CacheObjSpec struct {
	Attr string
}

func (c *CacheObj) DeepCopyObject() *CacheObj {
	return &CacheObj{
		TypeMeta: metav1.TypeMeta{
			APIVersion: c.APIVersion,
			Kind:       c.Kind,
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      c.Name,
			UID:       c.UID,
			Namespace: c.Namespace,
		},
		Spec: CacheObjSpec{
			Attr: c.Spec.Attr,
		},
	}
}

func cacheObjectKeyCreator(o interface{}) (bool, string, error) {
	co, ok := o.(*CacheObj)
	if !ok {
		return false, "", fmt.Errorf("unexpected type %T", co)
	}

	return true, co.Kind + "/" + co.Namespace + "/" + co.Name, nil
}

func altCacheObjectKeyCreator(o interface{}) (bool, string, error) {
	co, ok := o.(*CacheObj)
	if !ok {
		return false, "", fmt.Errorf("unexpected type %T", co)
	}

	return true, co.Kind + "/" + co.Namespace + "/" + co.Spec.Attr, nil
}

func newTestCache(t *testing.T, alternativeHierarchy bool) Cache {
	cache := &MemdbCache{}

	var err error
	specifiers := make([]IndexSpecifier, 0)
	specifiers = append(specifiers,
		IndexSpecifier{
			Name: idIndex,
			Indexer: &hierarchyIndex{
				keyCreator: cacheObjectKeyCreator,
			},
		})
	if alternativeHierarchy {
		specifiers = append(specifiers, IndexSpecifier{
			Name: altIndex,
			Indexer: &hierarchyIndex{
				keyCreator: altCacheObjectKeyCreator,
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

	retrieved, err := c.Get(idIndex, "CacheObject/ns1/ob1", nil)
	assert.NoError(t, err, "failed to retrieve object")

	assert.Equal(t, 1, len(retrieved), "Unexpected number of objects retrieved")
	assert.Equal(t, obj, retrieved[0], "Unexpected object")
}

func TestMemdbCache_Delete(t *testing.T) {
	c := newTestCache(t, false)

	obj := createObj("ns1", "ob1")
	err := c.Add(obj)
	assert.NoError(t, err, "failed to insert object")

	retrieved, err := c.Get(idIndex, "CacheObject/ns1/ob1", nil)
	assert.NoError(t, err, "failed to query object")
	assert.Equal(t, 1, len(retrieved), "failed to retrieve object")

	err = c.Delete(obj)
	assert.NoError(t, err, "failed to delete object")

	reretrieved, err := c.Get(idIndex, "CacheObject/ns1/ob1", nil)
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

	doSubsetTest(idIndex, "CacheObject", 0)
	doSubsetTest(idIndex, "CacheObject/ns1/a", 0)
	doSubsetTest(idIndex, "CacheObject/ns1/aa", 1)
}

func TestMemdbCache_Get(t *testing.T) {
	c := newTestCache(t, false)

	doSubsetTest := func(idxpath string, expected int) {
		subset, err := c.Get(idIndex, idxpath, nil)
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

	subset, err := c.Get(idIndex, "CacheObject", func(object interface{}) (bool, bool, error) {
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

	subset, err := c.Get(idIndex, "CacheObject", func(object interface{}) (bool, bool, error) {
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

	doSubsetTest(idIndex, "CacheObject", 2)
	doSubsetTest(idIndex, "CacheObject/ns1/foo", 2)
	doSubsetTest(idIndex, "CacheObject/ns1/foo1", 1)
	doSubsetTest(altIndex, "CacheObject/ns1/bar", 2)
	doSubsetTest(altIndex, "CacheObject/ns1/bar1", 1)
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

	doSubsetTest(idIndex, "CacheObject/ns1/aa", 1)
	doSubsetTest(altIndex, "CacheObject/ns1/bbc", 2)
	// Wrong index - won't delete anything
	doSubsetTest(idIndex, "CacheObject/ns1/bbc", 3)
}

func TestMemdbCache_Update(t *testing.T) {
	c := newTestCache(t, false)

	obj := createObj("ns1", "ob1")
	err := c.Add(obj)
	assert.NoError(t, err, "failed to insert object")

	err = c.Update(func(curr interface{}) (interface{}, error) {
		current := curr.(*CacheObj)
		upd := current.DeepCopyObject()
		upd.Spec.Attr = "upd"
		return upd, nil
	}, obj)
	assert.NoError(t, err, "failed to update object")

	retrieved, err := c.Get(idIndex, "CacheObject/ns1/ob1", nil)
	assert.NoError(t, err, "failed to retrieve object")

	assert.Equal(t, 1, len(retrieved), "Unexpected object")

	expected := obj.DeepCopyObject()
	expected.Spec.Attr = "upd"
	assert.Equal(t, expected, retrieved[0], "Unexpected object")
}

func TestMemdbCache_UpdateNotRealised(t *testing.T) {
	c := newTestCache(t, false)

	obj1 := createObj("ns1", "ob1")
	obj2 := createObj("ns1", "ob2")
	obj3 := createObj("ns1", "ob3")
	err := c.Add(obj1, obj2, obj3)
	assert.NoError(t, err, "failed to insert objects")

	err = c.Update(func(curr interface{}) (interface{}, error) {
		current := curr.(*CacheObj)
		if current.Name == "ob2" {
			return nil, nil
		}
		upd := current.DeepCopyObject()
		upd.Spec.Attr = "upd"
		return upd, nil
	}, obj1, obj2, obj3)
	assert.NoError(t, err, "failed to update object2")

	retrieved, err := c.Get(idIndex, "CacheObject/", nil)
	assert.NoError(t, err, "failed to retrieve objects")

	assert.Equal(t, 3, len(retrieved), "Unexpected object")
	withUpd := 0
	for _, retrieved := range retrieved {
		if retrieved.(*CacheObj).Spec.Attr == "upd" {
			withUpd++
		}
	}
	assert.Equal(t, 2, withUpd, "Unexpected number of objects with updated annotation")
}

func TestMemdbCache_UpdateAbsentObject(t *testing.T) {
	c := newTestCache(t, false)

	obj := createObj("ns1", "ob1")

	err := c.Update(func(curr interface{}) (interface{}, error) {
		assert.Fail(t, "unexpected invocation")
		return nil, nil
	}, obj)
	assert.Error(t, err, "expected update to fail")
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
