/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package resolvers

import (
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/google/uuid"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
)

func getMetric(name string, metrics []*consolegraphql.Metric) *consolegraphql.Metric {
	for _, m := range metrics {
		if m.Name == name {
			return m
		}
	}
	return nil
}

func createAddressSpace(addressspace, namespace string) *consolegraphql.AddressSpaceHolder {
	return &consolegraphql.AddressSpaceHolder{
		AddressSpace: v1beta1.AddressSpace {
			TypeMeta: metav1.TypeMeta{
				Kind: "AddressSpace",
			},
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressspace,
				Namespace: namespace,
				UID:       types.UID(uuid.New().String()),
			},
		},
	}
}

func createConnection(host, namespace, addressspace string, metrics ...*consolegraphql.Metric) *consolegraphql.Connection {
	return &consolegraphql.Connection{
		TypeMeta: metav1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      host,
			Namespace: namespace,
			UID:       types.UID(uuid.New().String()),
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressspace,
		},
		Metrics: metrics,
	}
}

func createAddress(namespace, name string, metrics... *consolegraphql.Metric) (*consolegraphql.AddressHolder) {
	return &consolegraphql.AddressHolder{
		Address: v1beta1.Address {
			TypeMeta: metav1.TypeMeta{
				Kind: "Address",
			},
			ObjectMeta: metav1.ObjectMeta{
				Name:      name,
				Namespace: namespace,
				UID:       types.UID(uuid.New().String()),
			},
		},
		Metrics: metrics,
	}
}
