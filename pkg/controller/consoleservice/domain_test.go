/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package consoleservice

import (
	"fmt"
	"testing"
)

func TestTopLevelDomainOnly(t *testing.T) {
	fqdns := append([]string{}, "m1a.com", "m2b.com")
	expected := ".com"
	doTest(t, &expected, fqdns)
}

func TestWithSubDomain(t *testing.T) {
	fqdns := append([]string{}, "m1a.mycompany.com", "m2b.mycompany.com")
	expected := ".mycompany.com"
	doTest(t, &expected, fqdns)
}

func TestNoMatch(t *testing.T) {
	fqdns := append([]string{}, "m1a.mycompany.org", "m2b.mycompany.com")
	doTest(t, nil, fqdns)
}

func TestNoFQDNs(t *testing.T) {
	doTest(t, nil, []string{})
}

func TestOneFQDN(t *testing.T) {
	fqdns := append([]string{}, "m1a.com")
	doTest(t, nil, fqdns)
}

func doTest(t *testing.T, expected *string, fqdns []string) {
	result := GetCommonDomain(fqdns)
	if expected == nil {
		if (result != nil) {
			t.Error(fmt.Sprintf("Unexpected common domain. Expecting nil got %s ", *result))
		}
	} else if *result != *expected {
		t.Error(fmt.Sprintf("Unexpected common domain. Expecting %s, got %s ", *expected, *result))
	}
}
