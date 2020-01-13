/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package consolegraphql

import (
	"github.com/google/go-cmp/cmp"
	"github.com/google/go-cmp/cmp/cmpopts"
	"github.com/stretchr/testify/assert"
	"testing"
	"time"
)

func TestRateCalculatingMetricValue(t *testing.T) {
	now := time.Now()
	metric := NewRateCalculatingMetric("foo", "mytype")
	metric.Update(0, now.Add(time.Minute  * - 1))
	err := metric.Update(1500, now)

	assert.NoError(t, err)
	assert.InDelta(t, float64(300), metric.Value, 0.1)
	// https://github.com/stretchr/testify/issues/502 - tracks ability to compare time with delta
	assert.True(t, cmp.Equal(now, metric.Time, cmpopts.EquateApproxTime(time.Second)))
}

func TestRateCalculatingMetricValueWithNoData(t *testing.T) {
	metric := NewRateCalculatingMetric("foo", "mytype")
	assert.Equal(t, float64(0), metric.Value)
}