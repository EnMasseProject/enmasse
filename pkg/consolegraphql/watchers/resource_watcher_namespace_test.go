/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package watchers

import (
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	v1 "k8s.io/api/core/v1"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/fake"
	fake2 "k8s.io/client-go/kubernetes/typed/core/v1/fake"
	"testing"
)

func newTestNamespaceWatcher(t *testing.T) *NamespaceWatcher {
	c := &cache.MemdbCache{}
	err := c.Init()
	if err != nil {
		t.Fatal("failed to create test resolver")
	}
	c.RegisterIndexCreator("Namespace", NamespaceIndexCreator)
	watcher := NamespaceWatcher{}

	err = watcher.Init(c, fake.NewSimpleClientset().CoreV1())
	if err != nil {
		t.Fatal("failed to create namespace watcher", err)
	}
	return &watcher
}

func TestWatchExistingValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := &v1.Namespace{
		ObjectMeta: v12.ObjectMeta{
			Name: "mynamespace",
		},
	}

	_, err := w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	if err != nil {
		t.Fatal("failed to create namespace", err)
	}

	err = w.Watch(v1.NamespaceAll)
	if err != nil {
		t.Fatal("failed to commence namespace watcher", err)
	}
	w.Shutdown()

	objs, err := w.Cache.Get("Namespace", nil)
	if err != nil {
		t.Fatal("failed query cache", err)
	}
	expected := 1
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of namespaces, expected %d, actual %d", expected, actual)
	}
}
func TestWatchCreateNewValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := &v1.Namespace{
		ObjectMeta: v12.ObjectMeta{
			Name: "mynamespace",
		},
	}

	err := w.Watch(v1.NamespaceAll)
	if err != nil {
		t.Fatal("failed to commence namespace1 watcher", err)
	}
	w.AwaitWatching()

	_, err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	if err != nil {
		t.Fatal("failed to create namespace", err)
	}
	w.Shutdown()

	objs, err := w.Cache.Get("Namespace", nil)
	if err != nil {
		t.Fatal("failed query cache", err)
	}
	expected := 1
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of namespaces, expected %d, actual %d", expected, actual)
	}
}

func TestWatchUpdateExistingValue(t *testing.T) {
	w := newTestNamespaceWatcher(t)

	namespace := &v1.Namespace{
		ObjectMeta: v12.ObjectMeta{
			Name: "mynamespace",
		},
	}

	created, err := w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Create(namespace)
	if err != nil {
		t.Fatal("failed to create namespace", err)
	}

	err = w.Watch(v1.NamespaceAll)
	if err != nil {
		t.Fatal("failed to commence namespace watcher", err)
	}
	w.AwaitWatching()

	copy := created.DeepCopy()
	if copy.Annotations == nil {
		copy.Annotations = make(map[string]string)
	}
	copy.Annotations["foo"] = "bar"

	_, err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Update(copy)
	if err != nil {
		t.Fatal("failed to update namespace", err)
	}
	w.Shutdown()

	objs, err := w.Cache.Get("Namespace", nil)
	if err != nil {
		t.Fatal("failed query cache", err)
	}
	expected := 1
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of namespaces, expected %d, actual %d", expected, actual)
	}
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
	if err != nil {
		t.Fatal("failed to create namespace", err)
	}

	err = w.Watch(v1.NamespaceAll)
	if err != nil {
		t.Fatal("failed to commence namespace watcher", err)
	}
	w.AwaitWatching()

	err = w.ClientInterface.Namespaces().(*fake2.FakeNamespaces).Delete(namespace.Name, &v12.DeleteOptions{})
	if err != nil {
		t.Fatal("failed to delete namespace", err)
	}
	w.Shutdown()

	objs, err := w.Cache.Get("Namespace", nil)
	if err != nil {
		t.Fatal("failed query cache", err)
	}
	expected := 0
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of namespaces, expected %d, actual %d", expected, actual)
	}
}
