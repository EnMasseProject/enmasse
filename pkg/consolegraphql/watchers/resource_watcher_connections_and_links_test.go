/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package watchers

import (
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
	v1meta "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes/fake"
	fake2 "k8s.io/client-go/kubernetes/typed/core/v1/fake"
	"testing"
	"time"
)

const addressSpace = "myaddrspace"
const namespace = "mynamespace"

func newTestConnectionsAndLinksWatcher(t *testing.T) (*ConnectionAndLinkWatcher, chan agent.AgentEvent) {
	objectCache := &cache.MemdbCache{}
	err := objectCache.Init(
		cache.IndexSpecifier{
			Name:    "id",
			Indexer: &cache.UidIndex{},
		},
		cache.IndexSpecifier{
			Name: "hierarchy",
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"Connection": ConnectionIndexCreator,
					"Link": ConnectionLinkIndexCreator,
				},
			},
		}, cache.IndexSpecifier{
			Name:         "addressLinkHierarchy",
			AllowMissing: true,
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"Link": AddressLinkIndexCreator,
				},
			},
		})
	assert.NoError(t, err)

	eventChan := make(chan agent.AgentEvent)

	watcher, err := NewConnectionAndLinkWatcher(objectCache, v1.NamespaceAll, MockAgentCollectorCreator(eventChan), ConnectionAndLinkWatcherClient(fake.NewSimpleClientset().CoreV1()))
	assert.NoError(t, err)

	return watcher, eventChan
}

func TestWatchConnection(t *testing.T) {
	w, eventChan := newTestConnectionsAndLinksWatcher(t)

	agentservice := &v1.Service{
		ObjectMeta: v1meta.ObjectMeta{
			Name: "myagentservice",
			Annotations: map[string]string{
				"addressSpace":          addressSpace,
				"addressSpaceNamespace": namespace,
			},
			Labels: map[string]string{
				"infraUuid": "abcdef",
				"app":       "enmasse",
				"component": "agent"},
		},
		Spec: v1.ServiceSpec{
			Ports: []v1.ServicePort{{
				Name: "amqps",
				Port: 5671,
			}},
		},
	}

	_, err := w.ClientInterface.Services("").(*fake2.FakeServices).Create(agentservice)
	assert.NoError(t, err)


	err = w.Watch()
	assert.NoError(t, err)

	eventChan <- agent.AgentEvent{
		Type: agent.AgentEventTypeRestart,
	}

	epoch := time.Now().Unix()
	connectionUid := uuid.New().String()
	linkUid := uuid.New().String()
	eventChan <- agent.AgentEvent{
		Type: agent.AgentConnectionEventType,
		Object: &agent.AgentConnection{
			Uuid:                  connectionUid,
			AddressSpace:          addressSpace,
			AddressSpaceNamespace: namespace,
			AddressSpaceType:      "standard",
			CreationTimestamp:     epoch,
			MessagesIn:            5,
			Senders: []agent.AgentAddressLink{
				{
					Uuid:     linkUid,
					Released: 6,
					Address:  "myaddr1",
				},
			},
		},
	}

	w.Shutdown()
	cons, err := w.Cache.Get("hierarchy", "Connection", nil)
	assert.NoError(t, err)

	assert.Equal(t, 1, len(cons), "Unexpected number of connections")

	actualConnection := cons[0].(*consolegraphql.Connection)
	expectedConnection := &consolegraphql.Connection{
		TypeMeta: v1meta.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: v1meta.ObjectMeta{
			Name:              connectionUid,
			Namespace:         namespace,
			UID:               types.UID(connectionUid),
			CreationTimestamp: v1meta.Unix(epoch, 0),
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressSpace,
			Protocol:     "amqp",
		},
	}

	conmetrics := actualConnection.Metrics
	actualConnection.Metrics = nil

	assert.Equal(t, expectedConnection, actualConnection, "Unexpected connection")

	links, err := w.Cache.Get("hierarchy", "Link", nil)
	assert.NoError(t, err)

	assert.Equal(t, 1, len(links), "Unexpected number of links")


	assert.Equal(t, 2, len(conmetrics), "Unexpected number of connection metrics")

	messagesInMetric := getMetric("enmasse_messages_in", conmetrics)
	assert.NotNil(t, messagesInMetric, "MessagesIn metric is absent")

	linkmetrics := links[0].(*consolegraphql.Link).Metrics

	assert.Equal(t, 8, len(linkmetrics), "Unexpected number of link metrics")

	releasedMetric := getMetric("enmasse_released", linkmetrics)
	assert.NotNil(t, releasedMetric, "Released metric is absent")
	assert.Equal(t, float64(6), releasedMetric.Value, "Unexpected released metric value")
}

func TestWatchConnectionWithChangingLinks(t *testing.T) {
	w, eventChan := newTestConnectionsAndLinksWatcher(t)

	agentservice := &v1.Service{
		ObjectMeta: v1meta.ObjectMeta{
			Name: "myagentservice",
			Annotations: map[string]string{
				"addressSpace":          addressSpace,
				"addressSpaceNamespace": namespace,
			},
			Labels: map[string]string{
				"infraUuid": "abcdef",
				"app":       "enmasse",
				"component": "agent"},
		},
		Spec: v1.ServiceSpec{
			Ports: []v1.ServicePort{{
				Name: "amqps",
				Port: 5671,
			}},
		},
	}


	_, err := w.ClientInterface.Services("").(*fake2.FakeServices).Create(agentservice)
	assert.NoError(t, err)

	err = w.Watch()
	assert.NoError(t, err)

	eventChan <- agent.AgentEvent{
		Type: agent.AgentEventTypeRestart,
	}

	connectionUid := uuid.New().String()
	sendingLinkUuid1 := uuid.New().String()
	sendingLinkUuid2 := uuid.New().String()
	receivingLinkUuid := uuid.New().String()
	eventChan <- agent.AgentEvent{
		Type: agent.AgentConnectionEventType,
		Object: &agent.AgentConnection{
			Uuid:                  connectionUid,
			AddressSpace:          addressSpace,
			AddressSpaceNamespace: namespace,
			AddressSpaceType:      "standard",
			Senders: []agent.AgentAddressLink{
				{
					Uuid:     sendingLinkUuid1,
					Address:  "myaddr1",
				},
				{
					Uuid:     sendingLinkUuid2,
					Address:  "myaddr1",
				},
			},
		},
	}

	eventChan <- agent.AgentEvent{
		Type: agent.AgentConnectionEventType,
		Object: &agent.AgentConnection{
			Uuid:                  connectionUid,
			AddressSpace:          addressSpace,
			AddressSpaceNamespace: namespace,
			AddressSpaceType:      "standard",
			Senders: []agent.AgentAddressLink{
				{
					Uuid:     sendingLinkUuid2,
					Address:  "myaddr1",
				},
			},
			Receivers: []agent.AgentAddressLink{
				{
					Uuid:     receivingLinkUuid,
					Address:  "myaddr1",
				},
			},
		},
	}

	w.Shutdown()
	objs, err := w.Cache.Get("hierarchy", "Connection", nil)
	assert.NoError(t, err)

	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of connections")

	links, err := w.Cache.Get("hierarchy", "Link", nil)
	assert.NoError(t, err)

	assert.Equal(t, 2, len(links), "Unexpected number of links")


	remainingSendingLink, err := w.Cache.Get("hierarchy", "Link", func(o interface{}) (bool, bool, error) {
		l := o.(*consolegraphql.Link)
		if l.Name == sendingLinkUuid2 {
			return true, false, nil
		} else {
			return false, true, nil
		}
	})
	assert.NoError(t, err)

	remainingSendingLinkMetrics := remainingSendingLink[0].(*consolegraphql.Link).Metrics
	assert.Equal(t, 8, len(remainingSendingLinkMetrics), "Unexpected number of link metrics for remaining sending link")

	newReceivingLink, err := w.Cache.Get("hierarchy", "Link", func(o interface{}) (bool, bool, error) {
		l := o.(*consolegraphql.Link)
		if l.Name == receivingLinkUuid {
			return true, false, nil
		} else {
			return false, true, nil
		}
	})

	assert.NoError(t, err)
	newReceivingLinkMetrics := newReceivingLink[0].(*consolegraphql.Link).Metrics
	assert.Equal(t, 8, len(newReceivingLinkMetrics), "Unexpected number of link metrics for removed sending link")
}

func TestWatchDeletedConnection(t *testing.T) {
	w, _ := newTestConnectionsAndLinksWatcher(t)

	agentservice := &v1.Service{
		ObjectMeta: v1meta.ObjectMeta{
			Name: "myagentservice",
			Annotations: map[string]string{
				"addressSpace":          addressSpace,
				"addressSpaceNamespace": namespace,
			},
			Labels: map[string]string{
				"infraUuid": "abcdef",
				"app":       "enmasse",
				"component": "agent"},
		},
		Spec: v1.ServiceSpec{
			Ports: []v1.ServicePort{{
				Name: "amqps",
				Port: 5671,
			}},
		},
	}

	eventChan := make(chan agent.AgentEvent)
	w.AgentCollectorCreator = MockAgentCollectorCreator(eventChan)

	_, err := w.ClientInterface.Services("").(*fake2.FakeServices).Create(agentservice)
	assert.NoError(t, err)

	err = w.Watch()
	assert.NoError(t, err)

	eventChan <- agent.AgentEvent{
		Type: agent.AgentEventTypeRestart,
	}

	connectionUid1 := uuid.New().String()
	eventChan <- agent.AgentEvent{
		Type: agent.AgentConnectionEventType,
		Object: &agent.AgentConnection{
			Uuid: connectionUid1,
		},
	}

	connectionUid2 := uuid.New().String()
	eventChan <- agent.AgentEvent{
		Type: agent.AgentConnectionEventType,
		Object: &agent.AgentConnection{
			Uuid: connectionUid2,
		},
	}

	eventChan <- agent.AgentEvent{
		Type: agent.AgentConnectionEventTypeDelete,
		Object: &agent.AgentConnection{
			Uuid: connectionUid1,
		},
	}

	w.Shutdown()
	objs, err := w.Cache.Get("hierarchy", "Connection", nil)
	assert.NoError(t, err)

	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of connections")

	actualConnectionUid := objs[0].(*consolegraphql.Connection).UID
	expectedConnectionUid := types.UID(connectionUid2)
	assert.Equal(t, expectedConnectionUid, actualConnectionUid, "Unexpected connection UID")
}

func TestWatchNewAgent(t *testing.T) {
	w, eventChan := newTestConnectionsAndLinksWatcher(t)

	err := w.Watch()
	assert.NoError(t, err)

	w.AwaitWatching()

	agentservice := &v1.Service{
		ObjectMeta: v1meta.ObjectMeta{
			Name: "myagentservice",
			Annotations: map[string]string{
				"addressSpace":          addressSpace,
				"addressSpaceNamespace": namespace,
			},
			Labels: map[string]string{
				"infraUuid": "abcdef",
				"app":       "enmasse",
				"component": "agent",
			},
		},
		Spec: v1.ServiceSpec{
			Ports: []v1.ServicePort{{
				Name: "amqps",
				Port: 5671,
			}},
		},
	}
	_, err = w.ClientInterface.Services("").(*fake2.FakeServices).Create(agentservice)
	assert.NoError(t, err)

	eventChan <- agent.AgentEvent{
		Type: agent.AgentEventTypeRestart,
	}

	connectionUid := uuid.New().String()
	eventChan <- agent.AgentEvent{
		Type: agent.AgentConnectionEventType,
		Object: &agent.AgentConnection{
			Uuid: connectionUid,
		},
	}

	w.Shutdown()
	objs, err := w.Cache.Get("hierarchy", "Connection", nil)
	assert.NoError(t, err)

	expected := 1
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of connections, expected %d, actual %d", expected, actual)
	}

}

type MockAgentCollector struct {
	eventChan chan agent.AgentEvent
	stopped   chan struct{}
}

func (m *MockAgentCollector) Collect(addressSpaceNamespace string, addressSpace string, infraUuid string, host string, port int32, handler agent.AgentEventHandler) error {
	go func() {
		defer close(m.stopped)
		for {
			select {
			case evt, ok := <-m.eventChan:
				if ok {
					evt.InfraUuid = infraUuid
					evt.AddressSpace = addressSpace
					evt.AddressSpaceNamespace = addressSpaceNamespace
					_ = handler(evt)
				} else {
					return
				}
			}
		}
	}()
	return nil
}

func (m *MockAgentCollector) Shutdown() {
	close(m.eventChan)
	<-m.stopped

}

func MockAgentCollectorCreator(events chan agent.AgentEvent) AgentCollectorCreator {
	return func() agent.AgentCollector {
		return &MockAgentCollector{
			eventChan: events,
			stopped:   make(chan struct{}),
		}
	}
}

func getMetric(name string, metrics []*consolegraphql.Metric) *consolegraphql.Metric {
	for _, m := range metrics {
		if m.Name == name {
			return m
		}
	}
	return nil
}
