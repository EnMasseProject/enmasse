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

// ObjectMutator is used with Cache.Update function to mutate existing
// object(s) in the cache within a transaction. The ObjectMutator function
// receives the current value it uses to return a replacement  Returning
// nil causes no change to be made for this value. Remaining objects will
// continue to be considered for update.
//
// It is illegal for the updater function to mutate the object's key(s).
type ObjectMutator = func(current interface{}) (replacement interface{}, e error)

func And(filters... ObjectFilter) ObjectFilter {
	return func(o interface{}) (bool, bool, error) {
		c := true
		for _, filter := range filters {
			if filter != nil {
				fr, fc, e := filter(o)
				if e != nil {
					return false, false, e
				}

				if !fc {
					c = false
				}
				if !fr {
					return false, fc, nil
				}
			}
		}

		return true, c, nil
	}
}

type Cache interface {
	Init(indexSpecifier ...IndexSpecifier) error

	Add(objs ...interface{}) error
	Update(mutator ObjectMutator, objs ...interface{}) error
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

func (r *MemdbCache) Update(mutator ObjectMutator, objs ...interface{}) error {
	schema := r.schema.Tables["object"].Indexes["id"]
	txn := r.db.Txn(true)
	defer txn.Abort()
	for _, o := range objs {
		if ivFound, key, err := getKeyForObject(o, schema); err == nil && ivFound {
			if current, err := txn.First("object", "id", key[:len(key) -1]); err == nil && ivFound && current != nil {
				mutated, err := mutator(current)
				if err != nil {
					return err
				}
				if mutated != nil {
					if newIvFound, mutatedKey,  err := getKeyForObject(mutated, schema); err == nil && newIvFound && mutatedKey == key {
						if err := txn.Insert("object", mutated); err != nil {
							return err
						}
					} else if err != nil {
						return err
					} else if !newIvFound {
						return fmt.Errorf("failed to find the index value on the mutated object [original key: %s, mutated object %+v]", key, mutated)
					} else if mutatedKey != key {
						return fmt.Errorf("update mutator must not change the key [original: %s mutated :%s]", key, mutatedKey)
					}
				}
			} else if err != nil {
				return err
			} else if current == nil {
				return fmt.Errorf("failed to find the object in the cache [key: %s]", key)
			}
		} else if err != nil {
			return err
		} else if ! ivFound {
			return fmt.Errorf("failed to find the index value on the object [object %+v]", o)
		}
	}
	txn.Commit()
	return nil
}

func getKeyForObject(obj interface{}, schema *memdb.IndexSchema) (bool, string, error) {
	b, bytes, err := schema.Indexer.(memdb.SingleIndexer).FromObject(obj)
	if err != nil {
		return false, "", err
	}
	return b, string(bytes), nil
}

func (r *MemdbCache) Get(idxName string, keyPrefix string, filter ObjectFilter) ([]interface{}, error) {
	schema := r.schema.Tables["object"].Indexes[idxName]

	txn := r.db.Txn(false)
	defer txn.Abort()
	objs := make([]interface{}, 0)
	if itr, err := txn.LowerBound("object", idxName, keyPrefix); err == nil {
		for obj := itr.Next(); obj != nil; obj = itr.Next() {

			if b, key, err := getKeyForObject(obj, schema); b && err == nil {
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
