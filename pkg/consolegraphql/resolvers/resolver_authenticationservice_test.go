/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"reflect"
	"testing"
)

func newTestAuthenticationServiceResolver(t *testing.T) *Resolver {
	c := &cache.MemdbCache{}
	err := c.Init(cache.IndexSpecifier{
		Name:    "id",
		Indexer: &cache.UidIndex{},
	},
		cache.IndexSpecifier{
			Name: "hierarchy",
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"AuthenticationService": watchers.AuthenticationServiceIndexCreator,
				},
			},
		})
	assert.NoError(t, err)

	resolver := Resolver{}
	resolver.Cache = c
	return &resolver

}

func TestQueryAuthenticationService(t *testing.T) {
	r := newTestAuthenticationServiceResolver(t)
	authenticationService := &v1beta1.AuthenticationService{
		TypeMeta: v12.TypeMeta{
			Kind: "AuthenticationService",
		},
		ObjectMeta: v12.ObjectMeta{
			Name: "none-authservice",
			UID:  types.UID(uuid.New().String()),
		},
	}
	err := r.Cache.Add(authenticationService)
	if err != nil {
		t.Fatal(err)
	}

	objs, err := r.Query().AuthenticationServices(context.TODO())
	if err != nil {
		t.Fatal(err)
	}


	if !reflect.DeepEqual(authenticationService.Spec, objs[0].Spec) {
		t.Fatalf("Unexpected authenticationService spec, expected %+v actual %+v", authenticationService.Spec, objs[0].Spec)
	}
	if !reflect.DeepEqual(authenticationService.ObjectMeta, objs[0].ObjectMeta) {
		t.Fatalf("Unexpected authenticationService object meta, expected %+v actual %+v", authenticationService.ObjectMeta, objs[0].ObjectMeta)
	}
}
