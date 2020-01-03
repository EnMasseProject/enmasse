/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package consolegraphql

import (
	"container/ring"
	"context"
	"github.com/prometheus/prometheus/pkg/labels"
	"github.com/prometheus/prometheus/pkg/timestamp"
	"github.com/prometheus/prometheus/prompb"
	"github.com/prometheus/prometheus/promql"
	"github.com/prometheus/prometheus/storage"
	"github.com/prometheus/prometheus/storage/remote"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"time"
)

type Connection struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ObjectMeta
	Spec ConnectionSpec
}

func (c Connection) SetGroupVersionKind(kind schema.GroupVersionKind) {
	panic("unused")
}

func (c Connection) GroupVersionKind() schema.GroupVersionKind {
	return schema.GroupVersionKind{
		Group:   "consoleapi.enmasse.io",
		Version: "beta1",
		Kind:    "Connection",
	}
}

func (c Connection) GetObjectKind() schema.ObjectKind {
	return c
}

func (c Connection) DeepCopyObject() runtime.Object {
	var newProps map[string]string
	if c.Spec.Properties != nil {
		newProps = make(map[string]string, len(c.Spec.Properties))
		for k, v := range c.Spec.Properties {
			newProps[k] = v
		}
	}
	return Connection{
		ObjectMeta: metav1.ObjectMeta{
			Name: c.Name,
			UID:  c.UID,
		},
		Spec: ConnectionSpec{
			AddressSpace: c.Spec.AddressSpace,
			Hostname:     c.Spec.Hostname,
			ContainerId:  c.Spec.ContainerId,
			Protocol:     c.Spec.Protocol,
			Properties:   newProps,
		},
	}
}

type ConnectionSpec struct {
	AddressSpace string
	Hostname     string
	ContainerId  string
	Protocol     string
	Encrypted    bool
	Properties   map[string]string
}

type Link struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ObjectMeta
	Spec LinkSpec
}

func (l Link) SetGroupVersionKind(kind schema.GroupVersionKind) {
	panic("unused")
}

func (l Link) GroupVersionKind() schema.GroupVersionKind {
	return schema.GroupVersionKind{
		Group:   "consoleapi.enmasse.io",
		Version: "beta1",
		Kind:    "Link",
	}
}

func (l Link) GetObjectKind() schema.ObjectKind {
	return l
}

func (l Link) DeepCopyObject() runtime.Object {
	return Link{
		ObjectMeta: metav1.ObjectMeta{
			Name: l.Name,
			UID:  l.UID,
		},
		Spec: LinkSpec{
			Connection:   l.Spec.Connection,
			Address:      l.Spec.Address,
			AddressSpace: l.Spec.AddressSpace,
			Role:         l.Spec.Role,
		},
	}
}

type LinkSpec struct {
	Connection   string
	AddressSpace string
	Address      string
	Role         string
}

type MetricValue interface {
	GetName() string
	GetUnit()  string
	GetType() string
	GetValue() (float64, time.Time, error)
	SetValue(float64, time.Time)
}

type Metric struct {
	// Make these map of label/value pairs to follow Prometheus conventions? ?
	Kind         string
	Namespace    string
	AddressSpace string
	Name         string

	// Used to additionally index link metrics by connection
	ConnectionName *string

	Value MetricValue
}


type simpleMetricValue struct {
	Name  string
	Type  string
	Value float64
	Unit  string
	Time  time.Time
}

func (s *simpleMetricValue) GetName() string {
	return s.Name
}

func (s *simpleMetricValue) GetType() string {
	return s.Type
}

func (s *simpleMetricValue) GetUnit() string {
	return s.Unit
}

func (s *simpleMetricValue) GetValue() (float64, time.Time, error) {
	return s.Value,s.Time, nil
}

func (s *simpleMetricValue) SetValue(v float64, t time.Time) {
	s.Value = v
	s.Time = t
}

func NewSimpleMetricValue(n string, t string, v float64, u string, ts time.Time) MetricValue {
	return &simpleMetricValue{
		Name:  n,
		Type:  t,
		Value: v,
		Unit:  u,
		Time:  ts,
	}
}

type rateCalculatingMetricValue struct {
	Name string
	Type string
	Unit string
	Ring *ring.Ring
}

type dataPointTimePair struct  {
	dataPoint float64
	ts        time.Time
}

func (t *rateCalculatingMetricValue) GetName() string {
	return t.Name
}

func (t *rateCalculatingMetricValue) GetUnit() string {
	return t.Unit
}

func (t *rateCalculatingMetricValue) GetType() string {
	return t.Type
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

func (aqr adaptingQuerier) LabelValues(name string) ([]string, storage.Warnings, error) {
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

func (aq adaptingQueryable) Querier(ctx context.Context, mint, maxt int64) (storage.Querier, error) {
	ts := make([]*prompb.TimeSeries, 0)

	samples := make([]prompb.Sample, 0)

	aq.dataPointRing.Do(func(rv interface{}) {
		if rv != nil {
			pair := rv.(dataPointTimePair)
			samples = append(samples, prompb.Sample{
				Value:                pair.dataPoint,
				Timestamp:            timestamp.FromTime(pair.ts),
			})
		}
	})

	labels := []prompb.Label{{Name: "unused_label", Value: "unused_value",},}
	ts = append(ts, &prompb.TimeSeries{
		Labels:  labels,
		Samples: samples,
	})
	qr := &prompb.QueryResult{
		Timeseries:           ts,
	}
	return &adaptingQuerier{
		qr,
		labels,
	}, nil
}

func (t *rateCalculatingMetricValue) GetValue() (float64, time.Time, error) {
	engine := promql.NewEngine(promql.EngineOpts{
		MaxConcurrent: 1,
		MaxSamples:    100,
		Timeout:       10 * time.Second,
	})
	now := time.Now()
	query, err := engine.NewInstantQuery(&adaptingQueryable{
		dataPointRing: t.Ring,
	}, "rate(unused_label[5m]) * 60", now)  // Rate per minute
	if err != nil {
		panic(err)
	}
	result := query.Exec(context.TODO())

	if result.Err != nil {
		return float64(0), now, err
	}

	vector := result.Value.(promql.Vector)
	if len(vector) == 0 {
		return float64(0), now, nil
	} else {
		return vector[0].V, timestamp.Time(vector[0].T), nil
	}
}

func (t *rateCalculatingMetricValue) SetValue(v float64, ts time.Time) {
	t.Ring.Value = dataPointTimePair{v, ts}
	t.Ring = t.Ring.Next()
}

func NewRateCalculatingMetricValue(n string, t string, u string) MetricValue {
	return &rateCalculatingMetricValue{
		Name: n,
		Type: t,
		Ring: ring.New(100),
		Unit: u,
	}
}
