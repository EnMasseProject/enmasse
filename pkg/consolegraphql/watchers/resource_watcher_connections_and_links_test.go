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
	v1 "k8s.io/api/core/v1"
	v1meta "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes/fake"
	fake2 "k8s.io/client-go/kubernetes/typed/core/v1/fake"
	"reflect"
	"testing"
	"time"
)

const addressSpace1 = "myaddrspace"
const namespace1 = "mynamespace"

func newTestConnectionsAndLinksWatcher(t *testing.T) *ConnectionAndLinkWatcher {
	c := &cache.MemdbCache{}
	err := c.Init()
	if err != nil {
		t.Fatal("failed to create test resolver")
	}
	c.RegisterIndexCreator("Connection", ConnectionIndexCreator)
	watcher := ConnectionAndLinkWatcher{}

	err = watcher.Init(c, fake.NewSimpleClientset().CoreV1())
	if err != nil {
		t.Fatal("failed to create connection and link watcher", err)
	}
	return &watcher
}

func TestWatchExistingAgentWithExistingConnection(t *testing.T) {
	w := newTestConnectionsAndLinksWatcher(t)

	agentservice := &v1.Service{
		ObjectMeta: v1meta.ObjectMeta{
			Name: "myagentservice",
			Annotations: map[string]string{
				"addressSpace":          addressSpace1,
				"addressSpaceNamespace": namespace1,
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

	eventChan := make(chan agent.AgentConnectionEvent)
	w.AgentCollectorCreator = MockAgentCollectorCreator(eventChan)

	_, err := w.ClientInterface.Services("").(*fake2.FakeServices).Create(agentservice)
	if err != nil {
		t.Fatal("failed to create agent service", err)
	}

	err = w.Watch(v1.NamespaceAll)
	if err != nil {
		t.Fatal("failed to commence agentservice watcher", err)
	}

	eventChan <- agent.AgentConnectionEvent{
		Type: agent.AgentConnectionEventTypeRestart,
	}

	epoch := time.Now().Unix()
	connectionUid := uuid.New().String()
	eventChan <- agent.AgentConnectionEvent{
		Type: agent.AgentConnectionEventTypeAdd,
		Object: &agent.AgentConnection{
			Uuid:              connectionUid,
			CreationTimestamp: epoch,
		},
	}

	w.Shutdown()
	objs, err := w.Cache.Get("Connection", nil)
	if err != nil {
		t.Fatal("failed query cache", err)
	}
	expected := 1
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of connections, expected %d, actual %d", expected, actual)
	}

	actualConnection := objs[0].(*consolegraphql.Connection)
	expectedConnection := &consolegraphql.Connection{
		TypeMeta: v1meta.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: v1meta.ObjectMeta{
			Name:              connectionUid,
			Namespace:         namespace1,
			UID:               types.UID(connectionUid),
			CreationTimestamp: v1meta.Unix(epoch, 0),
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressSpace1,
			Protocol:     "amqp",
		},
	}
	if !reflect.DeepEqual(expectedConnection, actualConnection) {
		t.Fatalf("Unexpected connection,\nexpected %+v,\nactual   %+v", expectedConnection, actualConnection)
	}

}

func TestWatchExistingAgentDeletesConnection(t *testing.T) {
	w := newTestConnectionsAndLinksWatcher(t)

	agentservice := &v1.Service{
		ObjectMeta: v1meta.ObjectMeta{
			Name: "myagentservice",
			Annotations: map[string]string{
				"addressSpace":          addressSpace1,
				"addressSpaceNamespace": namespace1,
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

	eventChan := make(chan agent.AgentConnectionEvent)
	w.AgentCollectorCreator = MockAgentCollectorCreator(eventChan)

	_, err := w.ClientInterface.Services("").(*fake2.FakeServices).Create(agentservice)
	if err != nil {
		t.Fatal("failed to create agent service", err)
	}

	err = w.Watch(v1.NamespaceAll)
	if err != nil {
		t.Fatal("failed to commence agentservice watcher", err)
	}

	eventChan <- agent.AgentConnectionEvent{
		Type: agent.AgentConnectionEventTypeRestart,
	}

	connectionUid1 := uuid.New().String()
	eventChan <- agent.AgentConnectionEvent{
		Type: agent.AgentConnectionEventTypeAdd,
		Object: &agent.AgentConnection{
			Uuid: connectionUid1,
		},
	}

	connectionUid2 := uuid.New().String()
	eventChan <- agent.AgentConnectionEvent{
		Type: agent.AgentConnectionEventTypeAdd,
		Object: &agent.AgentConnection{
			Uuid: connectionUid2,
		},
	}

	eventChan <- agent.AgentConnectionEvent{
		Type: agent.AgentConnectionEventTypeDelete,
		Object: &agent.AgentConnection{
			Uuid: connectionUid1,
		},
	}

	w.Shutdown()
	objs, err := w.Cache.Get("Connection", nil)
	if err != nil {
		t.Fatal("failed query cache", err)
	}
	expected := 1
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of connections, expected %d, actual %d", expected, actual)
	}

	actualConnectionUid := objs[0].(*consolegraphql.Connection).UID
	expectedConnectionUid := types.UID(connectionUid2)
	if actualConnectionUid != expectedConnectionUid {
		t.Fatalf("Unexpected connection expected: %s, actual %s", expectedConnectionUid, actualConnectionUid)
	}
}

func TestWatchNewAgent(t *testing.T) {
	w := newTestConnectionsAndLinksWatcher(t)

	eventChan := make(chan agent.AgentConnectionEvent)
	w.AgentCollectorCreator = MockAgentCollectorCreator(eventChan)

	err := w.Watch(v1.NamespaceAll)
	if err != nil {
		t.Fatal("failed to commence agentservice watcher", err)
	}

	w.AwaitWatching()

	agentservice := &v1.Service{
		ObjectMeta: v1meta.ObjectMeta{
			Name: "myagentservice",
			Annotations: map[string]string{
				"addressSpace":          addressSpace1,
				"addressSpaceNamespace": namespace1,
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
	if err != nil {
		t.Fatal("failed to create agent service", err)
	}

	eventChan <- agent.AgentConnectionEvent{
		Type: agent.AgentConnectionEventTypeRestart,
	}

	connectionUid := uuid.New().String()
	eventChan <- agent.AgentConnectionEvent{
		Type: agent.AgentConnectionEventTypeAdd,
		Object: &agent.AgentConnection{
			Uuid: connectionUid,
		},
	}

	w.Shutdown()
	objs, err := w.Cache.Get("Connection", nil)
	if err != nil {
		t.Fatal("failed query cache", err)
	}
	expected := 1
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of connections, expected %d, actual %d", expected, actual)
	}

}

type MockAgentCollector struct {
	eventChan chan agent.AgentConnectionEvent
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

func MockAgentCollectorCreator(events chan agent.AgentConnectionEvent) AgentCollectorCreator {
	return func() agent.AgentCollector {
		return &MockAgentCollector{
			eventChan: events,
			stopped:   make(chan struct{}),
		}
	}
}
