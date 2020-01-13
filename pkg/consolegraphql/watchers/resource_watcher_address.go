/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

// Code generated by go generate; DO NOT EDIT.

package watchers

import (
	"fmt"
	tp "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	cp "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/watch"
	"k8s.io/client-go/rest"
	"log"
	"reflect"
)

type AddressWatcher struct {
	Namespace       string
	cache.Cache
	ClientInterface cp.EnmasseV1beta1Interface
	watching        chan struct{}
    watchingStarted bool
	stopchan        chan struct{}
	stoppedchan     chan struct{}
    create          func(*tp.Address) interface{}
    update          func(*tp.Address, interface{}) bool
}

func NewAddressWatcher(c cache.Cache, options... WatcherOption) (ResourceWatcher, error) {

    kw := &AddressWatcher{
		Namespace:       v1.NamespaceAll,
		Cache:           c,
		watching:        make(chan struct{}),
		stopchan:        make(chan struct{}),
		stoppedchan:     make(chan struct{}),
		create:          func(v *tp.Address) interface{} {
                             return v
                         },
	    update:          func(v *tp.Address, e interface{}) bool {
                             if !reflect.DeepEqual(v, e) {
                                 *e.(*tp.Address) = *v
                                 return true
                             } else {
                                 return false
                             }
                         },
    }

    for _, option := range options {
        option(kw)
	}

	if kw.ClientInterface == nil {
		return nil, fmt.Errorf("Client must be configured using the AddressWatcherConfig or AddressWatcherClient")
	}
	return kw, nil
}

func AddressWatcherFactory(create func(*tp.Address) interface{}, update func(*tp.Address, interface{}) bool) WatcherOption {
	return func(watcher ResourceWatcher) error {
		w := watcher.(*AddressWatcher)
		w.create = create
        w.update = update
        return nil
	}
}

func AddressWatcherConfig(config *rest.Config) WatcherOption {
	return func(watcher ResourceWatcher) error {
		w := watcher.(*AddressWatcher)

		var cl interface{}
		cl, _  = cp.NewForConfig(config)

		client, ok := cl.(cp.EnmasseV1beta1Interface)
		if !ok {
			return fmt.Errorf("unexpected type %T", cl)
		}

		w.ClientInterface = client
        return nil
	}
}

// Used to inject the fake client set for testing purposes
func AddressWatcherClient(client cp.EnmasseV1beta1Interface) WatcherOption {
	return func(watcher ResourceWatcher) error {
		w := watcher.(*AddressWatcher)
		w.ClientInterface = client
        return nil
	}
}

func (kw *AddressWatcher) Watch() error {
	go func() {
		defer close(kw.stoppedchan)
		defer func() {
			if !kw.watchingStarted {
				close(kw.watching)
			}
		}()
		resource := kw.ClientInterface.Addresses(kw.Namespace)
		log.Printf("Address - Watching")
		running := true
		for running {
			err := kw.doWatch(resource)
			if err != nil {
				log.Printf("Address - Restarting watch")
			} else {
				running = false
			}
		}
		log.Printf("Address - Watching stopped")
	}()

	return nil
}

func (kw *AddressWatcher) AwaitWatching() {
	<-kw.watching
}

func (kw *AddressWatcher) Shutdown() {
	close(kw.stopchan)
	<-kw.stoppedchan
}

func (kw *AddressWatcher) doWatch(resource cp.AddressInterface) error {
	resourceList, err := resource.List(v1.ListOptions{})
	if err != nil {
		return err
	}

	curr, err := kw.Cache.GetMap("Address/", cache.UidKeyAccessor)

	var added = 0
	var updated = 0
	var unchanged = 0
	for _, res := range resourceList.Items {
		copy := res.DeepCopy()
		kw.updateKind(copy)

		if val, ok := curr[copy.UID]; ok {
			if kw.update(copy, val) {
				err = kw.Cache.Add(copy)
				updated++
			} else {
				unchanged++
			}
			delete(curr, copy.UID)
		} else {
			err = kw.Cache.Add(kw.create(copy))
			if err != nil {
				return err
			}
			added++
		}
	}

	// Now remove any stale
	for _, stale := range curr {
		err = kw.Cache.Delete(stale)
		if err != nil {
			return err
		}
	}
	var stale = len(curr)

	log.Printf("Address - Cache initialised population added %d, updated %d, unchanged %d, stale %d", added, updated, unchanged, stale)
	resourceWatch, err := resource.Watch(v1.ListOptions{
		ResourceVersion: resourceList.ResourceVersion,
	})

	if ! kw.watchingStarted {
		close(kw.watching)
		kw.watchingStarted = true
	}

	ch := resourceWatch.ResultChan()
	for {
		select {
		case event := <-ch:
			var err error
			if event.Type == watch.Error {
				err = fmt.Errorf("Watch ended in error")
			} else {
				res, ok := event.Object.(*tp.Address)
				log.Printf("Address - Received event type %s", event.Type)
				if !ok {
					err = fmt.Errorf("Watch error - object of unexpected type received")
				} else {
					copy := res.DeepCopy()
					kw.updateKind(copy)
					switch event.Type {
					case watch.Added:
						err = kw.Cache.Add(kw.create(copy))
					case watch.Modified:
						// TODO fix me
						curr, err := kw.Cache.GetMap("Address/", cache.UidKeyAccessor)
						if val, ok := curr[copy.UID]; ok {
							if kw.update(copy, val) {
								err = kw.Cache.Add(copy)
								updated++
								if err != nil {
									return err
								}
							} else {
								unchanged++
							}
							delete(curr, copy.UID)
						} else {
							err = kw.Cache.Add(kw.create(copy))
							added++
						}
					case watch.Deleted:
						err = kw.Cache.Delete(kw.create(copy))
					}
				}
			}
			if err != nil {
				return err
			}
		case <-kw.stopchan:
			log.Printf("Address - Shutdown received")
			return nil
		}
	}
}

func (kw *AddressWatcher) updateKind(o *tp.Address) {
	if o.TypeMeta.Kind == "" {
		o.TypeMeta.Kind = "Address"
	}
}
