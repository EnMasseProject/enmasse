/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package consolegraphql

import (
	"container/ring"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	authv1 "k8s.io/api/authorization/v1"
	"k8s.io/apimachinery/pkg/api/meta"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"time"
)

type HasMetrics interface {
	GetMetrics() []*Metric
}

type Connection struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              ConnectionSpec `json:"spec,omitempty"`
	Metrics           []*Metric      `json:"metrics"`
}

func (c *Connection) GetControllingResourceAttributes() *authv1.ResourceAttributes {
	return &authv1.ResourceAttributes{
		Resource:  "addressspaces",
		Group:     "enmasse.io",
		Version:   "v1beta1",
		Namespace: c.ObjectMeta.Namespace,
	}
}

func (c *Connection) GetMetrics() []*Metric {
	return c.Metrics
}

type ConnectionSpec struct {
	AddressSpace string            `json:"addressSpace,omitempty"`
	Hostname     string            `json:"hostname,omitempty"`
	ContainerId  string            `json:"containerId,omitempty"`
	Protocol     string            `json:"protocol,omitempty"`
	Encrypted    bool              `json:"encrypted,omitempty"`
	Properties   map[string]string `json:"properties,omitempty"`
}

type Link struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              LinkSpec  `json:"spec,omitempty"`
	Metrics           []*Metric `json:"metrics"`
}

func (l *Link) GetMetrics() []*Metric {
	return l.Metrics
}

type LinkSpec struct {
	Connection   string `json:"connection,omitempty"`
	AddressSpace string `json:"addressSpace,omitempty"`
	Address      string `json:"address,omitempty"`
	Role         string `json:"role,omitempty"`
}

type Metric struct {
	Name       string    `json:"name,omitempty"`
	Type       string    `json:"type,omitempty"`
	Value      float64   `json:"value,omitempty"`
	Unit       string    `json:"unit,omitempty"`
	Time       time.Time `json:"time,omitempty"`
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

func FindOrCreateRateCalculatingMetric(existing []*Metric, n string, t string, unit string) (*RateCalculatingMetric, []*Metric) {
	for _, m := range existing {
		if m.Name == n {
			return (*RateCalculatingMetric)(m), existing
		}
	}

	m := NewRateCalculatingMetric(n, t, unit)
	existing = append(existing, (*Metric)(m))
	return m, existing
}

func NewSimpleMetric(n string, t string) *SimpleMetric {
	metric := SimpleMetric{
		Name: n,
		Type: t,
	}
	return &metric
}

func (m *SimpleMetric) Update(v float64, ts time.Time) {
	m.Value = v
	m.Time = ts
	return
}

func NewRateCalculatingMetric(n string, t string, u string) *RateCalculatingMetric {
	m := RateCalculatingMetric{
		Name:       n,
		Type:       t,
		Unit:       u,
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
	v1beta1.AddressSpace `json:",inline"`
	Metrics              []*Metric `json:"metrics"`
}

func (ash *AddressSpaceHolder) GetMetrics() []*Metric {
	return ash.Metrics
}

func (ash *AddressSpaceHolder) GetControllingResourceAttributes() *authv1.ResourceAttributes {
	gvk := ash.TypeMeta.GroupVersionKind()
	return getResourceAttributes(gvk, ash.Namespace)
}

type AddressHolder struct {
	v1beta1.Address `json:",inline"`
	Metrics         []*Metric `json:"metrics"`
}

func (ah *AddressHolder) GetMetrics() []*Metric {
	return ah.Metrics
}

func (ah *AddressHolder) GetControllingResourceAttributes() *authv1.ResourceAttributes {
	gvk := ah.TypeMeta.GroupVersionKind()
	return getResourceAttributes(gvk, ah.Namespace)
}

func getResourceAttributes(gvk schema.GroupVersionKind, name string) *authv1.ResourceAttributes {
	return &authv1.ResourceAttributes{
		Resource:  kindToResource(gvk),
		Group:     gvk.Group,
		Version:   gvk.Version,
		Namespace: name,
	}
}

func kindToResource(gvk schema.GroupVersionKind) string {
	plural, _ := meta.UnsafeGuessKindToResource(gvk)
	resource := plural.Resource
	return resource
}
