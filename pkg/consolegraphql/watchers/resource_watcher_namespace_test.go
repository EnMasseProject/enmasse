/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package watchers

import (
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/fake"
	fake2 "k8s.io/client-go/kubernetes/typed/core/v1/fake"
	"testing"
)

func newTestNamespaceWatcher(t *testing.T) *NamespaceWatcher {
	c := &cache.MemdbCache{}
	err := c.Init(
		cache.IndexSpecifier{
			Name:    "id",
			Indexer: &cache.UidIndex{},
		},
		cache.IndexSpecifier{
			Name: "hierarchy",
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"Namespace": NamespaceIndexCreator,
				},
			},
		})
	assert.NoError(t, err, "failed to create test resolver")
	watcher, err := NewNamespaceWatcher(c, NamespaceWatcherClient(fake.NewSimpleClientset().CoreV1()))
	assert.NoError(t, err, "failed to create test resolver")

	return watcher.(*NamespaceWatcher)
}

func TestWatchExistingValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := &v1.Namespace{
		ObjectMeta: v12.ObjectMeta{
			Name: "mynamespace",
		},
	}

	_, err := w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	assert.NoError(t, err, "failed to create namespace")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence namespace watcher")
	w.Shutdown()

	objs, err := w.Cache.Get("hierarchy", "Namespace", nil)
	assert.NoError(t, err, "failed to query cache")
	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of namespaces")
}
func TestWatchCreateNewValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := &v1.Namespace{
		ObjectMeta: v12.ObjectMeta{
			Name: "mynamespace",
		},
	}

	err := w.Watch()
	assert.NoError(t, err, "failed to commence namespace watcher")
	w.AwaitWatching()

	_, err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	assert.NoError(t, err, "failed to create namespace")
	w.Shutdown()

	objs, err := w.Cache.Get("hierarchy", "Namespace", nil)
	assert.NoError(t, err, "failed to query cache")
	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of namespaces")
}

func TestWatchUpdateExistingValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := &v1.Namespace{
		ObjectMeta: v12.ObjectMeta{
			Name: "mynamespace",
		},
	}

	created, err := w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	assert.NoError(t, err, "failed to create namespace")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence namespace watcher")
	w.AwaitWatching()

	copy := created.DeepCopy()
	if copy.Annotations == nil {
		copy.Annotations = make(map[string]string)
	}
	copy.Annotations["foo"] = "bar"

	_, err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Update(copy)
	assert.NoError(t, err, "failed to update namespace")
	w.Shutdown()

	objs, err := w.Cache.Get("hierarchy", "Namespace", nil)
	assert.NoError(t, err, "failed to query cache")
	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of namespaces")
	retrieved, ok := objs[0].(*v1.Namespace)
	if !ok {
		t.Fatalf("Unexpected type %T", retrieved)
	}
	if val, ok := retrieved.Annotations["foo"]; !ok {
		t.Fatalf("Updated namespace lacks new annotation")
	} else if val != "bar" {
		t.Fatalf("Updated namespace has wrong annotation value")
	}

}

func TestWatchDeleteExistingValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := &v1.Namespace{
		ObjectMeta: v12.ObjectMeta{
			Name: "mynamespace",
		},
	}

	_, err := w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	assert.NoError(t, err, "failed to create namespace")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence namespace watcher")
	w.AwaitWatching()

	err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Delete(namespace.Name, &v12.DeleteOptions{})
	assert.NoError(t, err, "failed to delete namespace")
	w.Shutdown()

	objs, err := w.Cache.Get("hierarchy", "Namespace", nil)
	assert.NoError(t, err, "failed to query cache")
	expected := 0
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of namespaces")
}
