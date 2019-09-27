/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cache

import (
	"fmt"
	"github.com/hashicorp/go-memdb"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"strings"
)

type HierarchicalIndexCreator = func(runtime.Object) (string, error)
type ObjectFilter = func(runtime.Object) (bool, error)

type Cache interface {
	Init() error
	RegisterIndexCreator(string, HierarchicalIndexCreator)

	Add(objs ...runtime.Object) error
	Update(objs ...runtime.Object) error
	Delete(objs ...runtime.Object) error

	GetByUID(uid types.UID) (runtime.Object, error)
	Get(idxprefix string, filter ObjectFilter) ([]runtime.Object, error)
	GetMap(hidxpath string) (map[types.UID]runtime.Object, error)

	Dump() error
}

type MemdbCache struct {
	schema *memdb.DBSchema
	db     *memdb.MemDB
	index  *HierarchyIndex
}

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

type HierarchyIndex struct {
	indexCreators map[string]HierarchicalIndexCreator
}

func (s *HierarchyIndex) FromObjectStr(obj interface{}) (bool, string, error) {
	res, ok := obj.(runtime.Object)
	if !ok {
		return false, "", fmt.Errorf("unexpected type: %T", obj)
	}

	kind := res.GetObjectKind().GroupVersionKind().Kind
	if creator, ok := s.indexCreators[kind]; !ok {
		return false, "", fmt.Errorf("can't find hierarchical creator for kind %s", kind)
	} else {
		val, err := creator(res)
		if err != nil {
			return false, "", err
		}
		return true, val, nil
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

func (s *HierarchyIndex) RegisterIndexCreator(kind string, creator HierarchicalIndexCreator) {
	if s.indexCreators == nil {
		s.indexCreators = make(map[string]HierarchicalIndexCreator)
	}
	s.indexCreators[kind] = creator
}

func (r *MemdbCache) Init() error {
	r.index = &HierarchyIndex{}

	r.schema = &memdb.DBSchema{
		Tables: map[string]*memdb.TableSchema{
			"object": {
				Name: "object",
				Indexes: map[string]*memdb.IndexSchema{
					"id": {
						Name:    "id",
						Unique:  true,
						Indexer: &UidIndex{},
					},
					"hierarchy": {
						Name:    "hierarchy",
						Unique:  true,
						Indexer: r.index,
					},
				},
			},
		},
	}
	// Create a new data base
	var err error
	r.db, err = memdb.NewMemDB(r.schema)
	return err
}

func (r *MemdbCache) Add(objs ...runtime.Object) error {
	txn := r.db.Txn(true)
	defer txn.Abort()
	for _, o := range objs {
		if err := txn.Insert("object", o); err != nil {
			return err
		}
	}
	txn.Commit()
	return nil
}

func (r *MemdbCache) Update(objs ...runtime.Object) error {
	txn := r.db.Txn(true)
	defer txn.Abort()
	for _, o := range objs {
		res, ok := o.(v1.ObjectMetaAccessor)
		if !ok {
			return fmt.Errorf("unexpected type: %T", o)
		}
		meta := res.GetObjectMeta()
		if _, se := txn.First("object", "id", string(meta.GetUID())); se != nil {
			return se
		}
	}

	for _, o := range objs {
		if e := txn.Insert("object", o); e != nil {
			return e
		}
	}
	txn.Commit()
	return nil
}

func (r *MemdbCache) Delete(objs ...runtime.Object) error {
	txn := r.db.Txn(true)
	defer txn.Abort()
	for _, o := range objs {
		if err := txn.Delete("object", o); err != nil {
			return err
		}
	}
	txn.Commit()
	return nil
}

func (r *MemdbCache) GetByUID(uid types.UID) (runtime.Object, error) {
	txn := r.db.Txn(false)
	defer txn.Abort()
	if itr, err := txn.Get("object", "id", string(uid)); err == nil {
		for obj := itr.Next(); obj != nil; obj = itr.Next() {
			ro, ok := obj.(runtime.Object)
			if !ok {
				return nil, fmt.Errorf("unexpected type: %T", obj)
			}
			return ro, nil
		}
		return nil, err
	} else {
		return nil, err
	}
}

func (r *MemdbCache) Get(idxprefix string, filter ObjectFilter) ([]runtime.Object, error) {
	txn := r.db.Txn(false)
	defer txn.Abort()
	objs := make([]runtime.Object, 0)
	if itr, err := txn.LowerBound("object", "hierarchy", idxprefix); err == nil {
		for obj := itr.Next(); obj != nil; obj = itr.Next() {
			if b, idx, err := r.index.FromObjectStr(obj); b && err == nil {
				if strings.HasPrefix(idx, idxprefix) {
					ro, ok := obj.(runtime.Object)
					if !ok {
						return objs, fmt.Errorf("unexpected type: %T", obj)
					}
					if filter == nil {
						objs = append(objs, ro)
					} else {
						if ok, err := filter(ro); err == nil && ok {
							objs = append(objs, ro)
						} else if err != nil {
							return objs, fmt.Errorf("filter failed for type: %T", obj)
						}
					}
				} else {
					// End of results matching this index
					break
				}
			} else {
				return objs, fmt.Errorf("failed to generate hierarichal index for existing object: %T", obj)
			}
		}
		return objs, err
	} else {
		return objs, err
	}
}

func (r *MemdbCache) GetMap(kind string) (map[types.UID]runtime.Object, error) {
	objs, err := r.Get(kind, nil)
	if err != nil {
		return nil, err
	}
	mapped := make(map[types.UID]runtime.Object, len(objs))
	for _, obj := range objs {
		oma, ok := obj.(v1.ObjectMetaAccessor)
		if !ok {
			return nil, fmt.Errorf("unexpected type: %T", obj)
		}
		meta := oma.GetObjectMeta()
		mapped[meta.GetUID()] = obj
	}

	return mapped, nil
}

func (r *MemdbCache) RegisterIndexCreator(kind string, creator HierarchicalIndexCreator) {
	r.index.RegisterIndexCreator(kind, creator)
}

func (r *MemdbCache) Dump() error {
	txn := r.db.Txn(false)
	defer txn.Abort()
	fmt.Printf("============Cache Dump==============\n")
	if itr, err := txn.Get("object", "id"); err == nil {
		var summary = make(map[string]int)
		for obj := itr.Next(); obj != nil; obj = itr.Next() {
			res, ok := obj.(runtime.Object)
			if !ok {
				return fmt.Errorf("unexpected type: %T", obj)
			}

			gvk := res.GetObjectKind().GroupVersionKind()
			if _, ok := summary[gvk.Kind]; ok {
				summary[gvk.Kind] = summary[gvk.Kind] + 1
			} else {
				summary[gvk.Kind] = 1
			}
		}

		for k, v := range summary {
			fmt.Printf("%s => %d\n", k, v)
		}
		fmt.Printf("====================================\n")
	} else {
		return err
	}

	if itr, err := txn.Get("object", "hierarchy"); err == nil {
		var kind = ""
		for obj := itr.Next(); obj != nil; obj = itr.Next() {
			oma, ok := obj.(v1.ObjectMetaAccessor)
			if !ok {
				return fmt.Errorf("unexpected type: %T", obj)
			}
			ro, ok := obj.(runtime.Object)
			if !ok {
				return fmt.Errorf("unexpected type: %T", obj)
			}

			gvk := ro.GetObjectKind().GroupVersionKind()
			meta := oma.GetObjectMeta()

			if kind != gvk.Kind {
				fmt.Printf("====================================\n")
				fmt.Printf("Kind: %s\n", gvk.Kind)
				kind = gvk.Kind
			}
			fmt.Printf("%s %s %s\n", meta.GetUID(), meta.GetNamespace(), meta.GetName())
		}
		fmt.Printf("==========End Cache Dump============\n")
	} else {
		return err
	}
	return nil
}
