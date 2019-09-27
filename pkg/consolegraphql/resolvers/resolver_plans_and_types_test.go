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
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"reflect"
	"testing"
)

func newTestPlansResolver(t *testing.T) *Resolver {
	cache := &cache.MemdbCache{}
	err := cache.Init()
	if err != nil {
		t.Fatal("failed to create test resolver")
	}
	cache.RegisterIndexCreator("AddressPlan", watchers.AddressPlanIndexCreator)
	cache.RegisterIndexCreator("AddressSpacePlan", watchers.AddressSpacePlanIndexCreator)

	resolver := Resolver{}
	resolver.Cache = cache
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
	if err != nil {
		t.Fatal(err)
	}
	err = r.Cache.Add(asp2)
	if err != nil {
		t.Fatal(err)
	}

	objs, err := r.Query().AddressSpacePlans(context.TODO(), nil)
	if err != nil {
		t.Fatal(err)
	}

	expected := 2
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of addressspaceplans expected %d, actual %d", expected, actual)
	}
	if !reflect.DeepEqual(asp1, objs[0]) {
		t.Fatalf("Unexpected addressspaceplans expected %+v actual %+v", asp1, objs[0])
	}
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
	if err != nil {
		t.Fatal(err)
	}

	spaceType := AddressSpaceTypeStandard
	objs, err := r.Query().AddressSpacePlans(context.TODO(), &spaceType)
	if err != nil {
		t.Fatal(err)
	}

	expected := 1
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of addressspaceplans expected %d, actual %d", expected, actual)
	}
	if !reflect.DeepEqual(asp1, objs[0]) {
		t.Fatalf("Unexpected addressspaceplans expected %+v actual %+v", asp1, objs[0])
	}
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
	if err != nil {
		t.Fatal(err)
	}
	objs, err := r.Query().AddressPlans(context.TODO(), nil)
	if err != nil {
		t.Fatal(err)
	}

	expected := 2
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of addressplans expected %d, actual %d", expected, actual)
	}
	if !reflect.DeepEqual(ap1, objs[0]) {
		t.Fatalf("Unexpected addressplans expected %+v actual %+v", ap1, objs[0])
	}
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
	if err != nil {
		t.Fatal(err)
	}

	objs, err := r.Query().AddressPlans(context.TODO(), &asp.Name)
	if err != nil {
		t.Fatal(err)
	}

	expected := 2
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of addressplans expected %d, actual %d", expected, actual)
	}
}
