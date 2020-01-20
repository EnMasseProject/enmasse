/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package metric

import (
	"container/ring"
	"context"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/prometheus/prometheus/pkg/labels"
	"github.com/prometheus/prometheus/pkg/timestamp"
	"github.com/prometheus/prometheus/prompb"
	"github.com/prometheus/prometheus/promql"
	"github.com/prometheus/prometheus/storage"
	"github.com/prometheus/prometheus/storage/remote"
	"time"
)

type Calculator interface {
	Calc(timeSeries *ring.Ring) (float64, error)
}

type promQLCalculator struct {
	engine *promql.Engine
}

func New() (p *promQLCalculator) {
	return &promQLCalculator {
		engine: promql.NewEngine(promql.EngineOpts{
			MaxConcurrent: 1,
			MaxSamples:    100,
			Timeout:       10 * time.Second,
		}),
	}
}

func (p *promQLCalculator) Calc(timeSeries *ring.Ring) (float64, error) {

	now := time.Now()
	query, err := p.engine.NewInstantQuery(&adaptingQueryable{
		dataPointRing: timeSeries,
	}, "round(rate(unused_label[5m]) * 60)", now)  // Rate per minute
	if err != nil {
		return 0, err
	}
	result := query.Exec(context.TODO())

	if result.Err != nil {
		return 0, result.Err
	}

	vector := result.Value.(promql.Vector)
	if len(vector) == 0 {
		return 0, nil
	} else {
		return vector[0].V, nil
	}
}

type adaptingQueryable struct {
	dataPointRing *ring.Ring
}

type adaptingQuerier struct {
	queryResults *prompb.QueryResult
	labels []prompb.Label
}

func (aqr adaptingQuerier) Select(*storage.SelectParams, ...*labels.Matcher) (storage.SeriesSet, storage.Warnings, error) {
	return remote.FromQueryResult(aqr.queryResults), nil, nil
}

func (aqr adaptingQuerier) LabelValues(string) ([]string, storage.Warnings, error) {
	values := make([]string, len(aqr.labels))
	for i, v := range aqr.labels {
		values[i] = v.Value
	}
	return values, nil, nil
}

func (aqr adaptingQuerier) LabelNames() ([]string, storage.Warnings, error) {
	names := make([]string, len(aqr.labels))
	for i, v := range aqr.labels {
		names[i] = v.Name
	}
	return names, nil, nil
}

func (aqr adaptingQuerier) Close() error {
	return nil
}

func (aq adaptingQueryable) Querier(context.Context, int64, int64) (storage.Querier, error) {
	samples := make([]prompb.Sample, 0)

	aq.dataPointRing.Do(func(rv interface{}) {
		if rv != nil {
			pair := rv.(consolegraphql.DataPointTimePair)
			samples = append(samples, prompb.Sample{
				Value:                pair.DataPoint,
				Timestamp:            timestamp.FromTime(pair.Timestamp),
			})
		}
	})

	labs := []prompb.Label{{Name: "unused_label", Value: "unused_value",},}
	ts := make([]*prompb.TimeSeries, 0)
	ts = append(ts, &prompb.TimeSeries{
		Labels:  labs,
		Samples: samples,
	})
	qr := &prompb.QueryResult{
		Timeseries:           ts,
	}
	return &adaptingQuerier{
		qr,
		labs,
	}, nil
}
