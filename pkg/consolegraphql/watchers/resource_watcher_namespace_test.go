/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package watchers

import (
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/fake"
	fake2 "k8s.io/client-go/kubernetes/typed/core/v1/fake"
	"testing"
)

func newTestNamespaceWatcher(t *testing.T) *NamespaceWatcher {
	objectCache, err := cache.CreateObjectCache()
	assert.NoError(t, err, "failed to create object cache")
	watcher, err := NewNamespaceWatcher(objectCache, NamespaceWatcherClient(fake.NewSimpleClientset().CoreV1()))
	assert.NoError(t, err, "failed to create test resolver")

	return watcher.(*NamespaceWatcher)
}

func TestWatchNamespace_ListProvidesNewValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := createNamespace("mynamespace")

	_, err := w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	assert.NoError(t, err, "failed to create namespace")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence namespace watcher")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Namespace", nil)
	assert.NoError(t, err, "failed to query cache")
	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of namespaces")
}

func TestWatchNamespace_ListProvidesDifferingValues(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace1 := createNamespace("mynamespace1") // Will continue to exist unchanged.
	namespace2 := createNamespace("mynamespace2") // Will continue to exist, but kubernetes version will carry an update
	namespace3 := createNamespace("mynamespace3") // Will be provided new by kubernetes.
	namespace4 := createNamespace("mynamespace4") // Wont be provided by kubernetes, so will be removed.

	err := w.Cache.Add(namespace1.DeepCopyObject(), namespace2.DeepCopyObject(), namespace4.DeepCopyObject())
	assert.NoError(t, err, "failed to create namespace population")

	annotateNamespace(namespace2) // give namespace2 an update
	_, err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace1)
	assert.NoError(t, err, "failed to create namespace1")
	_, err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace2)
	assert.NoError(t, err, "failed to create namespace2")
	_, err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace3)
	assert.NoError(t, err, "failed to create namespace3")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence namespace watcher")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Namespace", nil)
	assert.NoError(t, err, "failed to query cache")

	assert.Equal(t, 3, len(objs), "Unexpected number of namespaces")

	cacheNamespaces := make(map[string]*v1.Namespace, 0)
	for i := range objs {
		ns := objs[i].(*v1.Namespace)
		cacheNamespaces[ns.Name] = ns
	}

	_, ns1present := cacheNamespaces[namespace1.Name]
	updatedNs2, ns2present := cacheNamespaces[namespace2.Name]
	_, ns3present := cacheNamespaces[namespace3.Name]
	_, ns4present := cacheNamespaces[namespace4.Name]
	assert.True(t, ns1present)
	assert.True(t, ns2present)
	assert.True(t, ns3present)
	assert.False(t, ns4present)

	assert.NotNil(t, updatedNs2.Annotations)
	assert.Equal(t, "bar", updatedNs2.Annotations["foo"])
}

func TestWatchNamespace_WatchCreatesNewValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := createNamespace("mynamespace")

	err := w.Watch()
	assert.NoError(t, err, "failed to commence namespace watcher")
	w.AwaitWatching()

	_, err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	assert.NoError(t, err, "failed to create namespace")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Namespace", nil)
	assert.NoError(t, err, "failed to query cache")
	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of namespaces")
}

func TestWatchNamespace_WatchUpdatesExistingValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := createNamespace("mynamespace")

	created, err := w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	assert.NoError(t, err, "failed to create namespace")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence namespace watcher")
	w.AwaitWatching()

	copy := created.DeepCopy()
	annotateNamespace(copy)

	_, err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Update(copy)
	assert.NoError(t, err, "failed to update namespace")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Namespace", nil)
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

func TestWatchNamespace_WatchDeletesExistingValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := createNamespace("mynamespace")

	_, err := w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	assert.NoError(t, err, "failed to create namespace")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence namespace watcher")
	w.AwaitWatching()

	err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Delete(namespace.Name, &metav1.DeleteOptions{})
	assert.NoError(t, err, "failed to delete namespace")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Namespace", nil)
	assert.NoError(t, err, "failed to query cache")
	expected := 0
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of namespaces")
}

func createNamespace(name string) *v1.Namespace {
	namespace := &v1.Namespace{
		ObjectMeta: metav1.ObjectMeta{
			Name: name,
		},
	}
	return namespace
}

func annotateNamespace(ns *v1.Namespace) {
	if ns.Annotations == nil {
		ns.Annotations = make(map[string]string)
	}
	ns.Annotations["foo"] = "bar"
}
