/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cchange

import (
	"crypto/sha256"
	"encoding/hex"
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

	if onlyKeys != nil {
		sort.Strings(onlyKeys)
	}

	for k, v := range data {
		if onlyKeys != nil {
			if sort.SearchStrings(onlyKeys, k) >= len(onlyKeys) {
				continue
			}
		}
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
