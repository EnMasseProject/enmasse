/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"testing"
)

func newTestPlansResolver(t *testing.T) *Resolver {
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
					"AddressPlan":      watchers.AddressPlanIndexCreator,
					"AddressSpacePlan": watchers.AddressSpacePlanIndexCreator,
				},
			},
		})
	assert.NoError(t, err, "failed to create test resolver")

	resolver := Resolver{}
	resolver.Cache = c
	return &resolver
}

func TestQueryAddressSpacePlansAll(t *testing.T) {
	r := newTestPlansResolver(t)
	asp1 := &v1beta2.AddressSpacePlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressSpacePlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressspaceplan1",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressSpacePlanSpec{
			AddressPlans:     []string{"my-addressspace-plan"},
			AddressSpaceType: "brokered",
			DisplayOrder:     0,
		},
	}
	asp2 := &v1beta2.AddressSpacePlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressSpacePlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressspaceplan2",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressSpacePlanSpec{
			AddressPlans:     []string{"my-addressspace-plan"},
			AddressSpaceType: "brokered",
			DisplayOrder:     1,
		},
	}
	err := r.Cache.Add(asp1)
	assert.NoError(t, err)

	err = r.Cache.Add(asp2)
	assert.NoError(t, err)

	objs, err := r.Query().AddressSpacePlans(context.TODO(), nil)
	assert.NoError(t, err)

	expected := 2
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of addressspaceplans")
	assert.Equal(t, asp1, objs[0], "Unexpected addressspaceplans")
}

func TestQueryAddressSpacePlansByType(t *testing.T) {
	r := newTestPlansResolver(t)
	asp1 := &v1beta2.AddressSpacePlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressSpacePlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressspaceplan1",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressSpacePlanSpec{
			AddressPlans:     []string{"my-addressspace-plan"},
			AddressSpaceType: "standard",
			DisplayOrder:     0,
		},
	}
	asp2 := &v1beta2.AddressSpacePlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressSpacePlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressspaceplan2",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressSpacePlanSpec{
			AddressPlans:     []string{"my-addressspace-plan"},
			AddressSpaceType: "brokered",
			DisplayOrder:     1,
		},
	}
	err := r.Cache.Add(asp1, asp2)
	assert.NoError(t, err)

	spaceType := AddressSpaceTypeStandard
	objs, err := r.Query().AddressSpacePlans(context.TODO(), &spaceType)
	assert.NoError(t, err)

	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of addressspaceplans")
	assert.Equal(t, asp1, objs[0], "Unexpected addressspaceplans")

}

func TestQueryAddressPlansAll(t *testing.T) {
	r := newTestPlansResolver(t)
	ap1 := &v1beta2.AddressPlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressPlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressplan1",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressPlanSpec{
			AddressType:  "queue",
			DisplayOrder: 0,
		},
	}
	ap2 := &v1beta2.AddressPlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressPlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressplan2",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressPlanSpec{
			AddressType:  "queue",
			DisplayOrder: 1,
		},
	}
	err := r.Cache.Add(ap1, ap2)
	assert.NoError(t, err)

	objs, err := r.Query().AddressPlans(context.TODO(), nil)
	assert.NoError(t, err)

	expected := 2
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of addressspaceplans")
	assert.Equal(t, ap1, objs[0], "Unexpected addressspaceplans")
}

func TestQueryAddressPlansByAddressSpacePlan(t *testing.T) {
	r := newTestPlansResolver(t)
	asp := &v1beta2.AddressSpacePlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressSpacePlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressspaceplan1",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressSpacePlanSpec{
			AddressPlans:     []string{"myaddressplan1", "myaddressplan2"},
			AddressSpaceType: "standard",
			DisplayOrder:     0,
		},
	}

	ap1 := &v1beta2.AddressPlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressPlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressplan1",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressPlanSpec{
			AddressType:  "topic",
			DisplayOrder: 0,
		},
	}
	ap2 := &v1beta2.AddressPlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressPlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressplan2",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressPlanSpec{
			AddressType:  "queue",
			DisplayOrder: 1,
		},
	}
	ap3 := &v1beta2.AddressPlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressPlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: "myaddressplan3",
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressPlanSpec{
			AddressType:  "queue",
			DisplayOrder: 1,
		},
	}
	err := r.Cache.Add(asp, ap1, ap2, ap3)
	assert.NoError(t, err)

	objs, err := r.Query().AddressPlans(context.TODO(), &asp.Name)
	assert.NoError(t, err)

	expected := 2
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of addressspaceplans")
}
