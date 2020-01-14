/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cache

import (
	"reflect"
	"testing"
	"time"

	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	userv1beta1informers "github.com/enmasseproject/enmasse/pkg/client/informers/externalversions/user/v1beta1"

	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"

	"k8s.io/client-go/kubernetes/scheme"
	toolscache "k8s.io/client-go/tools/cache"

	fake "sigs.k8s.io/controller-runtime/pkg/cache/informertest"
	"sigs.k8s.io/controller-runtime/pkg/client/apiutil"
)

func getGvk(t *testing.T, s *runtime.Scheme, obj runtime.Object) schema.GroupVersionKind {
	gvk, err := apiutil.GVKForObject(obj, s)
	if err != nil {
		t.Fatal("error creating GVK", err)
	}
	return gvk
}

func TestDelegation(t *testing.T) {
	s := scheme.Scheme
	user := &userv1beta1.MessagingUser{}
	s.AddKnownTypes(userv1beta1.SchemeGroupVersion, user)
	userGvk := getGvk(t, s, user)

	localCache := fake.FakeInformers{InformersByGVK: make(map[schema.GroupVersionKind]toolscache.SharedIndexInformer)}
	globalCache := fake.FakeInformers{InformersByGVK: make(map[schema.GroupVersionKind]toolscache.SharedIndexInformer)}

	// Set up mocks so that global cache returns something else
	globalCache.InformersByGVK[userGvk] = userv1beta1informers.NewMessagingUserInformer(nil, "", time.Second*1, nil)

	cache := delegateCache{
		defaultNamespace: "test",
		defaultCache:     &localCache,
		globalCache:      &globalCache,
		globalGvkMap:     make(map[schema.GroupVersionKind]bool),
		Scheme:           s,
	}

	localInformer, err := cache.GetInformer(user)
	if err != nil {
		t.Fatal("Error getting informer", err)
	}

	if reflect.TypeOf(localInformer).String() != "*controllertest.FakeInformer" {
		t.Error("Local informer is not the right type:", reflect.TypeOf(localInformer).String())
	}

	// Register MessagingUser in the global GVK map
	cache.globalGvkMap[userGvk] = true

	globalInformer, err := cache.GetInformer(user)
	if reflect.TypeOf(globalInformer).String() != "*cache.sharedIndexInformer" {
		t.Error("Global informer is not the right type:", reflect.TypeOf(globalInformer).String())
	}
}
