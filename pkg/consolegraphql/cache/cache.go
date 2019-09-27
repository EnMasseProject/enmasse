/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cache

import (
	"fmt"
	"github.com/hashicorp/go-memdb"
	"strings"
)

// ObjectFilters filter the objects return from a cache.
// return:
// - match, true if the object should be included in the results
// - cont, true if the traverse should continue
// - e in the event of a error preventing evaluation of the filter.
type ObjectFilter = func(interface{}) (match bool, cont bool, e error)

type Cache interface {
	Init(indexSpecifier ...IndexSpecifier) error

	Add(objs ...interface{}) error
	Delete(objs ...interface{}) error
	DeleteByPrefix(idxName, keyPrefix string) error

	Get(idxName string, keyPrefix string, filter ObjectFilter) ([]interface{}, error)
	GetMap(keyPrefix string, keyaccessor func(interface{}) (interface{}, error)) (map[interface{}]interface{}, error)
	Dump() error
}

type MemdbCache struct {
	schema *memdb.DBSchema
	db     *memdb.MemDB
}

type IndexSpecifier struct {
	Name string
	memdb.Indexer
	AllowMissing bool
}

func (r *MemdbCache) Init(indexSpecifiers ...IndexSpecifier) error {
	schemas := make(map[string]*memdb.IndexSchema)

	for _, is := range indexSpecifiers {
		schemas[is.Name] = &memdb.IndexSchema{
			Name:         is.Name,
			Unique:       true,
			AllowMissing: is.AllowMissing,
			Indexer:      is.Indexer,
		}
	}

	r.schema = &memdb.DBSchema{
		Tables: map[string]*memdb.TableSchema{
			"object": {
				Name:    "object",
				Indexes: schemas,
			},
		},
	}
	var err error
	r.db, err = memdb.NewMemDB(r.schema)
	return err
}

func (r *MemdbCache) Add(objs ...interface{}) error {
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

func (r *MemdbCache) Delete(objs ...interface{}) error {
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

func (r *MemdbCache) Get(idxName string, keyPrefix string, filter ObjectFilter) ([]interface{}, error) {
	schema := r.schema.Tables["object"].Indexes[idxName]

	getKeyForObject := func(obj interface{}) (bool, string, error) {
		b, bytes, err := schema.Indexer.(memdb.SingleIndexer).FromObject(obj)
		if err != nil {
			return false, "", err
		}
		return b, string(bytes), nil

	}
	txn := r.db.Txn(false)
	defer txn.Abort()
	objs := make([]interface{}, 0)
	if itr, err := txn.LowerBound("object", idxName, keyPrefix); err == nil {
		for obj := itr.Next(); obj != nil; obj = itr.Next() {

			if b, key, err := getKeyForObject(obj); b && err == nil {
				if strings.HasPrefix(key, keyPrefix) {
					if filter == nil {
						objs = append(objs, obj)
					} else {
						ok, cont, err := filter(obj)
						if err != nil {
							return objs, fmt.Errorf("filter failed for type: %T", obj)
						} else if ok {
							objs = append(objs, obj)
						}
						if !cont {
							break
						}
					}
				} else {
					// End of results matching this key prefix
					break
				}
			} else {
				return objs, fmt.Errorf("failed to generate key (index %s) for existing object: %T", idxName, obj)
			}
		}
		return objs, err
	} else {
		return objs, err
	}
}

func (r *MemdbCache) GetMap(keyPrefix string, keyaccessor func(interface{}) (interface{}, error)) (map[interface{}]interface{}, error) {
	objs, err := r.Get("hierarchy", keyPrefix, nil)
	if err != nil {
		return nil, err
	}
	mapped := make(map[interface{}]interface{}, len(objs))
	for _, obj := range objs {
		key, err := keyaccessor(obj)
		if err != nil {
			return nil, err
		}
		mapped[key] = obj
	}

	return mapped, nil
}

func (r *MemdbCache) DeleteByPrefix(idxName, keyPrefix string) error {
	txn := r.db.Txn(true)
	defer txn.Abort()
	if _, err := txn.DeletePrefix("object", idxName+"_prefix", keyPrefix); err != nil {
		return err
	}
	txn.Commit()
	return nil
}

func (r *MemdbCache) Dump() error {
	txn := r.db.Txn(false)
	defer txn.Abort()
	fmt.Printf("============Cache Dump==============\n")

	for name, indexSchema := range r.schema.Tables["object"].Indexes {
		fmt.Printf("Index: %s\n", name)

		if itr, err := txn.Get("object", name); err == nil {
			for obj := itr.Next(); obj != nil; obj = itr.Next() {
				_, key, err := indexSchema.Indexer.(memdb.SingleIndexer).FromObject(obj)
				if err != nil {
					return err
				}
				fmt.Printf("%s => %+v\n", key[:len(key)-1], obj)

			}
		} else {
			return err
		}
		fmt.Printf("End Index: %s\n\n", name)
	}
	return nil
}
