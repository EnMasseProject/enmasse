/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package consolegraphql

import (
	"container/ring"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"time"
)

type HasMetrics interface {
	GetMetrics() ([]*Metric)
}

type Connection struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ObjectMeta
	Spec    ConnectionSpec
	Metrics []*Metric
}

func (c *Connection) GetMetrics() []*Metric {
	return c.Metrics
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

func (l *Link) GetMetrics() []*Metric {
	return l.Metrics
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

type DataPointTimePair struct {
	DataPoint float64
	Timestamp time.Time
}

func (m *Metric) GetTimeSeries() *ring.Ring {
	return m.timeseries
}

func (m *Metric) AddTimeSeriesDataPoint(v float64, ts time.Time) {
	m.timeseries.Value = DataPointTimePair{v, ts}
	m.timeseries = m.timeseries.Next()
	m.Time = ts
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

func (m *SimpleMetric) Update(v float64, ts time.Time) {
	m.Value = v
	m.Time = ts
	return
}

func NewRateCalculatingMetric(n string, t string) *RateCalculatingMetric {
	m := RateCalculatingMetric{
		Name:       n,
		Type:       t,
		timeseries: ring.New(100),
	}
	return &m
}

func (m *RateCalculatingMetric) Update(v float64, ts time.Time) {
	m.timeseries.Value = DataPointTimePair{v, ts}
	m.timeseries = m.timeseries.Next()
	m.Time = ts
	return
}

type AddressSpaceHolder struct {
	v1beta1.AddressSpace
	Metrics     []*Metric
}

func (ash *AddressSpaceHolder) GetMetrics() []*Metric {
	return ash.Metrics
}


type AddressHolder struct {
	v1beta1.Address
	Metrics     []*Metric
}

func (ah *AddressHolder) GetMetrics() []*Metric {
	return ah.Metrics
}
