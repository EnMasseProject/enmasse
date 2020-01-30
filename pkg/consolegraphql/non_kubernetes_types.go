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
	"k8s.io/apimachinery/pkg/types"
	"time"
)

type HasMetrics interface {
	GetMetrics() []*Metric
}

type Metadata struct {
	Name string `json:"name,omitempty"`
	Namespace string `json:"namespace,omitempty"`
	SelfLink string `json:"selfLink,omitempty"`
	UID types.UID `json:"uid,omitempty"`
	ResourceVersion string `json:"resourceVersion,omitempty"`
	Generation int64 `json:"generation,omitempty"`
	CreationTimestamp metav1.Time `json:"creationTimestamp,omitempty"`
	Annotations map[string]string `json:"annotations,omitempty"`
}


type Connection struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ObjectMeta
	Spec    ConnectionSpec `json:"spec"`
	Metrics []*Metric      `json:"metrics,omitempty"`
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
	AddressSpace string            `json:"addressSpace"`
	Hostname     string            `json:"hostname"`
	ContainerId  string            `json:"containerId"`
	Protocol     string            `json:"protocol"`
	Encrypted    bool              `json:"encrypted"`
	Properties   map[string]string `json:"properties,omitempty"`
}

type Link struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              LinkSpec  `json:"spec,omitempty"`
	Metrics           []*Metric `json:"metrics,omitempty"`
}

func (l *Link) GetMetrics() []*Metric {
	return l.Metrics
}

type LinkSpec struct {
	Connection   string `json:"connection"`
	AddressSpace string `json:"addressSpace"`
	Address      string `json:"address"`
	Role         string `json:"role"`
}

type Metric struct {
	Name       string     `json:"name"`
	Type       string     `json:"type"`
	Value      float64    `json:"value"`
	Unit       string     `json:"unit"`
	Time       time.Time  `json:"time"`
	timeseries *ring.Ring `json:"timeseries"`
}

type DataPointTimePair struct {
	DataPoint float64   `json:"dataPoint"`
	Timestamp time.Time `json:"timestamp"`
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
	v1beta1.AddressSpace
	Metrics []*Metric `json:"metric,omitempty"`
}

func (ash *AddressSpaceHolder) GetMetrics() []*Metric {
	return ash.Metrics
}

func (ash *AddressSpaceHolder) GetControllingResourceAttributes() *authv1.ResourceAttributes {
	gvk := ash.TypeMeta.GroupVersionKind()
	return getResourceAttributes(gvk, ash.Namespace)
}

type AddressHolder struct {
	v1beta1.Address
	Metrics []*Metric `json:"metrics,omitempty"`
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
