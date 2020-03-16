/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cchange

import (
	"crypto/sha256"
	"encoding/hex"
	"github.com/enmasseproject/enmasse/pkg/util"
	"hash"
	"sort"
)

type ConfigChangeRecorder struct {
	hasher hash.Hash
}

func NewRecorder() *ConfigChangeRecorder {
	return &ConfigChangeRecorder{
		hasher: sha256.New(),
	}
}

func (c *ConfigChangeRecorder) AddStringsFromMap(data map[string]string, onlyKeys ...string) {

	// extract keys, and sort
	keys := make([]string, len(data))
	for k, _ := range data {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	// sort keys in order to use SearchStrings later on
	if onlyKeys != nil {
		sort.Strings(onlyKeys)
	}

	// iterate with sorted keys
	for _, k := range keys {
		if onlyKeys != nil {
			if !util.ContainsString(onlyKeys, k) {
				continue
			}
		}
		v := data[k]
		c.hasher.Write([]byte(v))
	}

}

func (c *ConfigChangeRecorder) AddString(data string) {
	c.hasher.Write([]byte(data))
}

func (c ConfigChangeRecorder) Hash() []byte {
	return c.hasher.Sum(nil)
}

func (c ConfigChangeRecorder) HashString() string {
	return hex.EncodeToString(c.Hash())
}

func (c ConfigChangeRecorder) Clone() *ConfigChangeRecorder {

	// start a new hash
	new := sha256.New()
	// taking the parent into consideration
	new.Write(c.hasher.Sum(nil))

	// return the new
	return &ConfigChangeRecorder{
		hasher: new,
	}
}
