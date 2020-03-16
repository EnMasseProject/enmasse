/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import "sort"

// Searches a **sorted** for a string value.
func ContainsString(strings []string, value string) bool {
	idx := sort.SearchStrings(strings, value)
	return idx < len(strings) && strings[idx] == value
}
