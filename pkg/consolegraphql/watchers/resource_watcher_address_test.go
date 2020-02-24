/*
* Copyright 2020, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package watchers

import (
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/fake"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"testing"
)

func newTestAddressWatcher(t *testing.T) *AddressWatcher {
	objectCache, err := cache.CreateObjectCache()
	assert.NoError(t, err, "failed to create object cache")

	clientset := fake.NewSimpleClientset()

	watcher, err := NewAddressWatcher(objectCache, nil, AddressWatcherClient(clientset.EnmasseV1beta1()),
		AddressWatcherFactory(AddressCreate, AddressUpdate))
	assert.NoError(t, err, "failed to create test resolver")

	return watcher.(*AddressWatcher)
}

func TestWatchAddress_ListProvidesNewValue(t *testing.T) {
	w := newTestAddressWatcher(t)

	namespace := "mynamespace"
	addr := createAddress(namespace, "myyaddressspace.myaddr")

	_, err := w.ClientInterface.Addresses(namespace).Create(&addr.Address)
	assert.NoError(t, err, "failed to create address")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence address watcher")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Address", nil)
	assert.NoError(t, err, "failed to query cache")
	assert.Equal(t, 1, len(objs), "Unexpected number of addresses")
	assert.Equal(t, int32(0), w.GetRestartCount())

	received := objs[0].(*consolegraphql.AddressHolder)
	assert.NotEmpty(t, received.TypeMeta.APIVersion)
	assert.NotEmpty(t, received.TypeMeta.Kind)
}

func TestWatchAddress_ListProvidesDifferingValues(t *testing.T) {
	w := newTestAddressWatcher(t)

	addr1 := createAddress(namespace, "myyaddressspace.myaddr1") // Will continue to exist unchanged.
	addr2 := createAddress(namespace, "myyaddressspace.myaddr2") // Will continue to exist, but kubernetes version will carry an update
	addr3 := createAddress(namespace, "myyaddressspace.myaddr3") // Will be provided new by kubernetes.
	addr4 := createAddress(namespace, "myyaddressspace.myaddr4") // Wont be provided by kubernetes, so will be removed.

	err := w.Cache.Add(deepCopyAddress(addr1), deepCopyAddress(addr2), deepCopyAddress(addr4))
	assert.NoError(t, err, "failed to create address population")

	annotateAddress(&addr2.Address) // give namespace2 an update
	_, err = w.ClientInterface.Addresses(namespace).Create(&addr1.Address)
	assert.NoError(t, err, "failed to create address1")
	_, err = w.ClientInterface.Addresses(namespace).Create(&addr2.Address)
	assert.NoError(t, err, "failed to create address2")
	_, err = w.ClientInterface.Addresses(namespace).Create(&addr3.Address)
	assert.NoError(t, err, "failed to create address3")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence address watcher")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Address", nil)
	assert.NoError(t, err, "failed to query cache")

	assert.Equal(t, 3, len(objs), "Unexpected number of addresses")

	cacheAddrs := make(map[string]*consolegraphql.AddressHolder, 0)
	for i := range objs {
		ns := objs[i].(*consolegraphql.AddressHolder)
		cacheAddrs[ns.Name] = ns
	}

	_, ns1present := cacheAddrs[addr1.Name]
	updatedNs2, ns2present := cacheAddrs[addr2.Name]
	_, ns3present := cacheAddrs[addr3.Name]
	_, ns4present := cacheAddrs[addr4.Name]
	assert.True(t, ns1present)
	assert.True(t, ns2present)
	assert.True(t, ns3present)
	assert.False(t, ns4present)

	assert.NotNil(t, updatedNs2.Annotations)
	assert.Equal(t, "bar", updatedNs2.Annotations["foo"])
	assert.Equal(t, int32(0), w.GetRestartCount())
}

func TestWatchAddress_WatchCreatesNewValue(t *testing.T) {
	w := newTestAddressWatcher(t)

	namespace := "mynamespace"
	addr := createAddress(namespace, "myyaddressspace.myaddr")

	err := w.Watch()
	assert.NoError(t, err, "failed to commence address watcher")
	w.AwaitWatching()

	_, err = w.ClientInterface.Addresses(namespace).Create(&addr.Address)
	assert.NoError(t, err, "failed to create address")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Address", nil)
	assert.NoError(t, err, "failed to query cache")
	assert.Equal(t, 1, len(objs), "Unexpected number of addresses")
	assert.Equal(t, int32(0), w.GetRestartCount())

	received := objs[0].(*consolegraphql.AddressHolder)
	assert.NotEmpty(t, received.TypeMeta.APIVersion)
	assert.NotEmpty(t, received.TypeMeta.Kind)
}

func TestWatchAddress_WatchUpdatesExistingValue(t *testing.T) {
	w := newTestAddressWatcher(t)

	namespace := "mynamespace"
	addr := createAddress(namespace, "myyaddressspace.myaddr")

	created, err := w.ClientInterface.Addresses(namespace).Create(&addr.Address)
	assert.NoError(t, err, "failed to create address")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence address watcher")
	w.AwaitWatching()

	copy := created.DeepCopy()
	annotateAddress(copy)

	_, err = w.ClientInterface.Addresses(namespace).Update(copy)
	assert.NoError(t, err, "failed to update address")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Address", nil)
	assert.NoError(t, err, "failed to query cache")
	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of addresses")
	retrieved, ok := objs[0].(*consolegraphql.AddressHolder)
	if !ok {
		t.Fatalf("Unexpected type %T", retrieved)
	}
	if val, ok := retrieved.Annotations["foo"]; !ok {
		t.Fatalf("Updated address lacks new annotation")
	} else if val != "bar" {
		t.Fatalf("Updated address has wrong annotation value")
	}
	assert.Equal(t, int32(0), w.GetRestartCount())
}

func TestWatchAddress_WatchDeletesExistingValue(t *testing.T) {
	w := newTestAddressWatcher(t)

	namespace := "mynamespace"
	addr := createAddress(namespace, "myyaddressspace.myaddr")

	_, err := w.ClientInterface.Addresses(namespace).Create(&addr.Address)
	assert.NoError(t, err, "failed to create address")

	err = w.Watch()
	assert.NoError(t, err, "failed to commence address watcher")
	w.AwaitWatching()

	err = w.ClientInterface.Addresses(namespace).Delete(addr.Name, &metav1.DeleteOptions{})
	assert.NoError(t, err, "failed to address namespace")
	w.Shutdown()

	objs, err := w.Cache.Get(cache.PrimaryObjectIndex, "Address", nil)
	assert.NoError(t, err, "failed to query cache")
	expected := 0
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of addresses")
	assert.Equal(t, int32(0), w.GetRestartCount())
}

func annotateAddress(addr *v1beta1.Address) {
	if addr.Annotations == nil {
		addr.Annotations = make(map[string]string)
	}
	addr.Annotations["foo"] = "bar"
}

func deepCopyAddress(ah *consolegraphql.AddressHolder) *consolegraphql.AddressHolder {
	metrics := ah.Metrics
	object := ah.DeepCopyObject().(*v1beta1.Address)
	if ah.Metrics != nil {
		metrics = make([]*consolegraphql.Metric, len(ah.Metrics))
		for i := range ah.Metrics {
			if ah.Metrics[i] != nil {
				metrics[i] = &consolegraphql.Metric{}
				*metrics[i] = *ah.Metrics[i]
			}
		}
	}
	return &consolegraphql.AddressHolder{
		Address: *object,
		Metrics: ah.Metrics,
	}
}
