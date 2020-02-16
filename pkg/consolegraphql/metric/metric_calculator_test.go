/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package metric

import (
	"container/ring"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/stretchr/testify/assert"
	"testing"
	"time"
)

func TestMetric_CalcValue(t *testing.T) {

	now := time.Now()
	calculator := New("round(rate(unused_label[5m]), 0.01)")

	timeSeries := buildTimeSeries(
		consolegraphql.DataPointTimePair{
			DataPoint: float64(0),
			Timestamp: now.Add(time.Minute * -1)},
		consolegraphql.DataPointTimePair{
			DataPoint: float64(1500),
			Timestamp: now},
	)

	value, err := calculator.Calc(timeSeries)
	assert.NoError(t, err)
	assert.InDelta(t, float64(5), value, 0.1)
}

func TestMetric_NoData(t *testing.T) {

	calculator := New("round(rate(unused_label[5m]), 0.01)")
	timeSeries := buildTimeSeries()

	value, err := calculator.Calc(timeSeries)
	assert.NoError(t, err)
	assert.InDelta(t, float64(0), value, 0.1)
}

func buildTimeSeries(pairs ...consolegraphql.DataPointTimePair) *ring.Ring {
	timeSeries := ring.New(5)
	for _, pair := range pairs {
		timeSeries.Value = pair
		timeSeries = timeSeries.Next()
	}
	return timeSeries
}
