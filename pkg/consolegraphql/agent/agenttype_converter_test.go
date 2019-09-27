/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package agent

import (
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"testing"
	"time"
)

func TestConvertConnectionOnly(t *testing.T) {

	epoch := time.Now().Unix()
	connectionUid := uuid.New().String()
	namespace := "namespace1"
	addressSpace := "addressSpace1"

	connection := &AgentConnection{
		Uuid:                  connectionUid,
		CreationTimestamp:     epoch,
		AddressSpace:          addressSpace,
		AddressSpaceNamespace: namespace,
		Encrypted:             true,
	}

	actualConnection, _, _ := ToConnectionK8Style(connection)

	expectedConnection := &consolegraphql.Connection{
		TypeMeta: metav1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:              connectionUid,
			Namespace:         namespace,
			UID:               types.UID(connectionUid),
			CreationTimestamp: metav1.Unix(epoch, 0),
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressSpace,
			Protocol:     "amqps",
			Encrypted:    true,
		},
	}

	assert.Equal(t, expectedConnection, actualConnection, "expected and actual connections unequal")
}

func TestConvertConnectionMetrics(t *testing.T) {

	epoch := time.Now().Unix()
	connectionUid := uuid.New().String()
	namespace := "namespace1"
	addressSpace := "addressSpace1"

	connection := &AgentConnection{
		Uuid:                  connectionUid,
		CreationTimestamp:     epoch,
		AddressSpace:          addressSpace,
		AddressSpaceNamespace: namespace,
		Encrypted:             true,
		MessagesIn:            10,
		MessagesOut:           20,
	}

	_, _, metrics := ToConnectionK8Style(connection)

	messageInMetric := getMetric("enmasse_messages_in", metrics)
	expectedMessageInMetric := &consolegraphql.Metric{
		Kind:         "Connection",
		Namespace:    namespace,
		AddressSpace: addressSpace,
		Name:         connectionUid,
		MetricName:   "enmasse_messages_in",
		MetricType:   "gauge",
		MetricValue:  10,
	}

	assert.Equal(t, expectedMessageInMetric, messageInMetric, "expected and actual connection metric unequal")
}

func TestConvertStandardConnectionWithLink(t *testing.T) {

	epoch := time.Now().Unix()
	connectionUid := uuid.New().String()
	linkUid := uuid.New().String()
	namespace := "namespace1"
	addressSpace := "addressSpace1"
	address := "foo"

	connection := &AgentConnection{
		Uuid:                  connectionUid,
		CreationTimestamp:     epoch,
		AddressSpace:          addressSpace,
		AddressSpaceNamespace: namespace,
		AddressSpaceType:      "standard",
		Senders: []AgentAddressLink{
			{
				Uuid:     linkUid,
				Address:  address,
				Released: 10,
			},
		},
	}

	_, objs, metrics := ToConnectionK8Style(connection)
	actualLink := objs[0]

	expectedLink := &consolegraphql.Link{
		TypeMeta: metav1.TypeMeta{
			Kind: "Link",
		},
		ObjectMeta: metav1.ObjectMeta{
			UID:       types.UID(linkUid),
			Name:      linkUid,
			Namespace: namespace,
		},
		Spec: consolegraphql.LinkSpec{
			Connection:   connection.Uuid,
			AddressSpace: addressSpace,
			Address:      address,
			Role:         "sender",
		},
	}

	assert.Equal(t, expectedLink, actualLink, "expected and actual links unequal")

	releasedMetric := getMetric("enmasse_released", metrics)
	expectedReleasedMetric := &consolegraphql.Metric{
		Kind:           "Link",
		Namespace:      namespace,
		AddressSpace:   addressSpace,
		Name:           linkUid,
		ConnectionName: &connectionUid,
		MetricName:     "enmasse_released",
		MetricType:     "counter",
		MetricValue:    10,
	}

	assert.Equal(t, expectedReleasedMetric, releasedMetric, "expected and actual link metric unequal")
}

func TestConvertBrokeredConnectionWithLink(t *testing.T) {

	epoch := time.Now().Unix()
	connectionUid := uuid.New().String()
	linkUid := uuid.New().String()
	namespace := "namespace1"
	addressSpace := "addressSpace1"
	address := "foo"

	connection := &AgentConnection{
		Uuid:                  connectionUid,
		CreationTimestamp:     epoch,
		AddressSpace:          addressSpace,
		AddressSpaceNamespace: namespace,
		AddressSpaceType:      "brokered",
		Senders: []AgentAddressLink{
			{
				Uuid:       linkUid,
				Address:    address,
				Deliveries: 30,
			},
		},
	}

	_, objs, metrics := ToConnectionK8Style(connection)
	actualLink := objs[0]

	expectedLink := &consolegraphql.Link{
		TypeMeta: metav1.TypeMeta{
			Kind: "Link",
		},
		ObjectMeta: metav1.ObjectMeta{
			UID:       types.UID(linkUid),
			Name:      linkUid,
			Namespace: namespace,
		},
		Spec: consolegraphql.LinkSpec{
			Connection:   connection.Uuid,
			AddressSpace: addressSpace,
			Address:      address,
			Role:         "sender",
		},
	}

	assert.Equal(t, expectedLink, actualLink, "expected and actual links unequal")

	releasedMetric := getMetric("enmasse_deliveries", metrics)
	expectedDeliveriesMetric := &consolegraphql.Metric{
		Kind:           "Link",
		Namespace:      namespace,
		AddressSpace:   addressSpace,
		Name:           linkUid,
		ConnectionName: &connectionUid,
		MetricName:     "enmasse_deliveries",
		MetricType:     "counter",
		MetricValue:    30,
	}

	assert.Equal(t, expectedDeliveriesMetric, releasedMetric, "expected and actual link metric unequal")
}


func getMetric(name string, metrics []*consolegraphql.Metric) *consolegraphql.Metric {
	for _, m := range metrics {
		if m.MetricName == name {
			return m
		}
	}
	return nil
}
