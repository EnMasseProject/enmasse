/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"testing"
)

func TestAddressName(t *testing.T) {
	for _, v := range []struct {
		addressName string
		expected    string
	}{
		{addressName: "telemetry/iot-project-ns.iot1", expected: "telemetryiot-project-ns-4cf002ae-37a5-38d6-9749-b7f85b29a385"},
		{addressName: "event/iot-project-ns.iot1", expected: "eventiot-project-nsiot1-82203f07-4081-392b-a834-dd1ffcaa2c5f"},
	} {

		out := EncodeAddressSpaceAsMetaName(v.addressName)
		if out != v.expected {
			t.Errorf("Encoding error - input: '%s' expected: '%s', actual: '%s'", v.addressName, v.expected, out)
		}

	}
}
