/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package consoleservice

import (
	"strings"
)

func GetCommonDomain(all_fqdns []string) (*string, int) {

	if len(all_fqdns) <= 1 {
		return nil, 0;
	}

	first_fqdn := strings.Split(all_fqdns[0], ".")
	remaining_fqdns := all_fqdns[1:]

	common := make([]string, 0)

	matching := true
	for len(first_fqdn) > 0 && matching {
		common = append(first_fqdn[len(first_fqdn) - 1:], common...)
		first_fqdn = first_fqdn[:len(first_fqdn) -1 ]

		trial_common_subdomain := "." + strings.Join(common, ".")
		for _, other_fqdn := range remaining_fqdns {
			if !strings.HasSuffix(other_fqdn, trial_common_subdomain) {
				common = common[1:]
				matching = false
				break
			}
		}
	}

	if len(common) == 0 {
		return nil, 0
	} else {
		common_sub := "." + strings.Join(common,".")
		return &common_sub, len(common)
	}
}
