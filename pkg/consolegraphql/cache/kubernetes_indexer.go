/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package cache

import (
	"fmt"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
)

type UidIndex struct {
}

func (s *UidIndex) FromObject(obj interface{}) (bool, []byte, error) {
	var val string

	res, ok := obj.(v1.ObjectMetaAccessor)
	if !ok {
		return false, nil, fmt.Errorf("unexpected type: %T", obj)
	}

	meta := res.GetObjectMeta()
	val = string(meta.GetUID())
	val += "\x00"
	return true, []byte(val), nil
}

func (s *UidIndex) FromArgs(args ...interface{}) ([]byte, error) {
	if len(args) != 1 {
		return nil, fmt.Errorf("must provide only a single argument")
	}
	arg, ok := args[0].(string)
	if !ok {
		return nil, fmt.Errorf("argument must be a string: %#v", args[0])
	}
	// Add the null character as a terminator
	arg += "\x00"
	return []byte(arg), nil
}

type HierarchicalIndexCreator = func(runtime.Object) (string, error)

type HierarchyIndex struct {
	IndexCreators map[string]HierarchicalIndexCreator
}

func (s *HierarchyIndex) FromObjectStr(obj interface{}) (bool, string, error) {
	res, ok := obj.(runtime.Object)
	if !ok {
		return false, "", fmt.Errorf("unexpected type: %T", obj)
	}

	kind := res.GetObjectKind().GroupVersionKind().Kind
	if creator, ok := s.IndexCreators[kind]; !ok {
		return false, "", nil
	} else {
		key, err := creator(res)
		return true, key, err
	}
}

func (s *HierarchyIndex) FromObject(obj interface{}) (bool, []byte, error) {
	b, val, e := s.FromObjectStr(obj)
	if b && e == nil {
		val += "\x00"
		return true, []byte(val), nil
	} else {
		return false, []byte{}, e
	}
}

func (s *HierarchyIndex) FromArgs(args ...interface{}) ([]byte, error) {
	if len(args) != 1 {
		return nil, fmt.Errorf("must provide only a single argument")
	}
	arg, ok := args[0].(string)
	if !ok {
		return nil, fmt.Errorf("argument must be a string: %#v", args[0])
	}
	// Add the null character as a terminator
	arg += "\x00"
	return []byte(arg), nil
}

func (s *HierarchyIndex) PrefixFromArgs(args ...interface{}) ([]byte, error) {
	if len(args) != 1 {
		return nil, fmt.Errorf("must provide only a single argument")
	}
	arg, ok := args[0].(string)
	if !ok {
		return nil, fmt.Errorf("argument must be a string: %#v", args[0])
	}
	return []byte(arg), nil
}

func UidKeyAccessor(obj interface{}) (interface{}, error) {
	oma, ok := obj.(v1.ObjectMetaAccessor)
	if !ok {
		return nil, fmt.Errorf("unexpected type: %T", obj)
	}
	meta := oma.GetObjectMeta()
	return meta.GetUID(), nil
}
