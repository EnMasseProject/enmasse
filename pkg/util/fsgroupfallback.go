/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package util

import (
	"encoding/json"
)

var fsGroupFallbackMap map[string]int64

func lazyLoadFsGroupFallbackMap() {
	if fsGroupFallbackMap != nil {
		return
	}

	jsonStr := GetEnvOrDefault("FS_GROUP_FALLBACK_MAP", "{}")

	var data = make(map[string]int64)
	if err := json.Unmarshal([]byte(jsonStr), &data); err != nil {
		panic(err)
	}
	fsGroupFallbackMap = data
}

func GetFsGroupOverride(component string) *int64 {
	lazyLoadFsGroupFallbackMap()
	namespace := GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	if i, ok := fsGroupFallbackMap[component]; ok && namespace == "openshift-operators" {
		return &i
	} else {
		return nil
	}
}
