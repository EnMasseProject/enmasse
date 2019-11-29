/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cache

import (
	"context"

	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/client-go/rest"
	"sigs.k8s.io/controller-runtime/pkg/cache"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client/apiutil"
	rlog "sigs.k8s.io/controller-runtime/pkg/log"
)

var log = rlog.Log.WithName("delegate_cache")

// DelegateCacheBuilder - Builder function to create a new delegate cache.  This cache allows
// mapping GVK to be served by a global cache while falling back to a default namespaced cache
// for other types.
func NewDelegateCacheBuilder(defaultNamespace string, globalGvks ...schema.GroupVersionKind) cache.NewCacheFunc {
	return func(config *rest.Config, opts cache.Options) (cache.Cache, error) {
		opts.Namespace = defaultNamespace
		defaultCache, err := cache.New(config, opts)
		if err != nil {
			return nil, err
		}

		opts.Namespace = ""
		globalCache, err := cache.New(config, opts)
		if err != nil {
			return nil, err
		}

		globalGvkMap := make(map[schema.GroupVersionKind]bool)
		for _, gvk := range globalGvks {
			globalGvkMap[gvk] = true
		}
		return &delegateCache{
			defaultNamespace: defaultNamespace,
			defaultCache:     defaultCache,
			globalCache:      globalCache,
			globalGvkMap:     globalGvkMap,
			Scheme:           opts.Scheme}, nil
	}
}

type delegateCache struct {
	defaultNamespace string
	defaultCache     cache.Cache
	globalCache      cache.Cache
	globalGvkMap     map[schema.GroupVersionKind]bool
	Scheme           *runtime.Scheme
}

var _ cache.Cache = &delegateCache{}

// Methods for delegateCache to conform to the Informers interface
func (c *delegateCache) GetInformer(obj runtime.Object) (cache.Informer, error) {
	gvk, err := apiutil.GVKForObject(obj, c.Scheme)
	if err != nil {
		return nil, err
	}

	if c.globalGvkMap[gvk] {
		return c.globalCache.GetInformer(obj)
	} else {
		return c.defaultCache.GetInformer(obj)
	}
}

func (c *delegateCache) GetInformerForKind(gvk schema.GroupVersionKind) (cache.Informer, error) {
	if c.globalGvkMap[gvk] {
		return c.globalCache.GetInformerForKind(gvk)
	} else {
		return c.defaultCache.GetInformerForKind(gvk)
	}

}

func (c *delegateCache) Start(stopCh <-chan struct{}) error {
	startFn := func(cacheType string, cache cache.Cache) {
		err := cache.Start(stopCh)
		if err != nil {
			log.Error(err, "delegate cache failed to start cache", "cacheType", cacheType)
		}
	}

	go startFn("namespaced", c.defaultCache)
	go startFn("global", c.globalCache)

	<-stopCh
	return nil
}

func (c *delegateCache) WaitForCacheSync(stop <-chan struct{}) bool {
	synced := true
	if s := c.defaultCache.WaitForCacheSync(stop); !s {
		synced = s
	}

	if s := c.globalCache.WaitForCacheSync(stop); !s {
		synced = s
	}
	return synced
}

func (c *delegateCache) IndexField(obj runtime.Object, field string, extractValue client.IndexerFunc) error {
	gvk, err := apiutil.GVKForObject(obj, c.Scheme)
	if err != nil {
		return err
	}
	if c.globalGvkMap[gvk] {
		if err := c.globalCache.IndexField(obj, field, extractValue); err != nil {
			return err
		}
	} else {
		if err := c.defaultCache.IndexField(obj, field, extractValue); err != nil {
			return err
		}
	}
	return nil
}

func (c *delegateCache) Get(ctx context.Context, key client.ObjectKey, obj runtime.Object) error {
	gvk, err := apiutil.GVKForObject(obj, c.Scheme)
	if err != nil {
		return err
	}

	if c.globalGvkMap[gvk] {
		return c.globalCache.Get(ctx, key, obj)
	} else {
		return c.defaultCache.Get(ctx, key, obj)
	}
}

func (c *delegateCache) List(ctx context.Context, list runtime.Object, opts ...client.ListOption) error {
	gvk, err := apiutil.GVKForObject(list, c.Scheme)
	if err != nil {
		return err
	}

	if c.globalGvkMap[gvk] {
		return c.globalCache.List(ctx, list, opts...)
	} else {
		return c.defaultCache.List(ctx, list, opts...)
	}
}
