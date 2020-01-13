/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package consolegraphql

import (
	"container/ring"
	"context"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
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
	Spec    ConnectionSpec
	Metrics []*Metric
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
	Metrics []*Metric
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

type Metric struct {
	Name  string
	Type  string
	Value float64
	Unit  string
	Time  time.Time
	timeseries *ring.Ring
}

type SimpleMetric Metric
type RateCalculatingMetric Metric

func FindOrCreateSimpleMetric(existing []*Metric, n string, t string) (*SimpleMetric, []*Metric) {
	for _, m := range existing {
		if m.Name == n {
			return (*SimpleMetric)(m), existing
		}
	}

	m := NewSimpleMetric(n, t)
	existing = append(existing, (*Metric)(m))
	return m, existing
}

func FindOrCreateRateCalculatingMetric(existing []*Metric, n string, t string) (*RateCalculatingMetric, []*Metric) {
	for _, m := range existing {
		if m.Name == n {
			return (*RateCalculatingMetric)(m), existing
		}
	}

	m := NewRateCalculatingMetric(n, t)
	existing = append(existing, (*Metric)(m))
	return m, existing
}


func NewSimpleMetric(n string, t string) *SimpleMetric {
	metric := SimpleMetric{
		Name:  n,
		Type:  t,
	}
	return &metric
}

func (m *SimpleMetric) Update(v float64, ts time.Time) error {
	m.Value = v
	m.Time = ts
	return nil
}

func NewRateCalculatingMetric(n string, t string) *RateCalculatingMetric {
	m := RateCalculatingMetric{
		Name:       n,
		Type:       t,
		timeseries: ring.New(100),
	}
	return &m
}

func (m *RateCalculatingMetric) Update(v float64, ts time.Time) error {
	m.timeseries.Value = dataPointTimePair{v, ts}
	m.timeseries = m.timeseries.Next()
	m.Time = ts
	return m.updateMetricValue()
}

func (m *RateCalculatingMetric) updateMetricValue() error {

	engine := promql.NewEngine(promql.EngineOpts{
		MaxConcurrent: 1,
		MaxSamples:    100,
		Timeout:       10 * time.Second,
	})
	now := time.Now()
	query, err := engine.NewInstantQuery(&adaptingQueryable{
		dataPointRing: m.timeseries,
	}, "round(rate(unused_label[5m]) * 60)", now)  // Rate per minute
	if err != nil {
		return err
	}
	result := query.Exec(context.TODO())

	if result.Err != nil {
		return result.Err
	}

	vector := result.Value.(promql.Vector)
	if len(vector) == 0 {
		m.Value = 0
		m.Time = now
	} else {
		m.Value = vector[0].V
		m.Time = now
	}
	return nil
}



type dataPointTimePair struct  {
	dataPoint float64
	ts        time.Time
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




type AddressSpaceHolder struct {
	v1beta1.AddressSpace
	Metrics     []*Metric
}

func (ash AddressSpaceHolder) SetGroupVersionKind(kind schema.GroupVersionKind) {
	panic("unused")
}

func (ash AddressSpaceHolder) GroupVersionKind() schema.GroupVersionKind {
	return ash.AddressSpace.GroupVersionKind()
}

func (ash AddressSpaceHolder) GetObjectKind() schema.ObjectKind {
	return ash.AddressSpace.GetObjectKind()
}

func (ash AddressSpaceHolder) DeepCopyObject() runtime.Object {
	return AddressSpaceHolder{
		AddressSpace: *ash.AddressSpace.DeepCopy(),
	}
}

func (ash AddressSpaceHolder) GetObjectMeta() metav1.Object {
	return ash.AddressSpace.GetObjectMeta()
}

type AddressHolder struct {
	v1beta1.Address
	Metrics     []*Metric
}

func (ah AddressHolder) SetGroupVersionKind(kind schema.GroupVersionKind) {
	panic("unused")
}

func (ah AddressHolder) GroupVersionKind() schema.GroupVersionKind {
	return ah.Address.GroupVersionKind()
}

func (ah AddressHolder) GetObjectKind() schema.ObjectKind {
	return ah.Address.GetObjectKind()
}

func (ah AddressHolder) DeepCopyObject() runtime.Object {
	return AddressHolder{
		Address: *ah.Address.DeepCopy(),
	}
}

func (ah AddressHolder) GetObjectMeta() metav1.Object {
	return ah.Address.GetObjectMeta()
}
