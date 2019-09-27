/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package consolegraphql

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
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

type Metric struct {
	// Make these map of label/value pairs to follow ?
	Kind         string
	Namespace    string
	AddressSpace string
	Name         string

	// Used to additionally index link metrics by connection
	ConnectionName *string

	MetricName  string
	MetricType  string
	MetricValue float64
	MetricUnit  string
}
