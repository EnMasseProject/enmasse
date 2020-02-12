/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"testing"
)

func newTestPlansResolver(t *testing.T) *Resolver {
	objectCache, err := cache.CreateObjectCache()
	assert.NoError(t, err, "failed to create object cache")

	resolver := Resolver{}
	resolver.Cache = objectCache
	return &resolver
}

func TestQueryAddressSpacePlansAll(t *testing.T) {
	r := newTestPlansResolver(t)
	asp1 := createAddressSpacePlan("brokered", "myaddressspaceplan1", 0, []string{"my-addressspace-plan"})
	asp2 := createAddressSpacePlan("brokered", "myaddressspaceplan2", 1, []string{"my-addressspace-plan"})
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
	asp1 := createAddressSpacePlan("standard", "myaddressspaceplan1", 0, []string{"my-addressspace-plan"})
	asp2 := createAddressSpacePlan("brokered", "myaddressspaceplan2", 0, []string{"my-addressspace-plan"})

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
	ap1 := createAddressPlan("myaddressplan1", "queue", 0)
	ap2 := createAddressPlan("myaddressplan2", "queue", 1)
	err := r.Cache.Add(ap1, ap2)
	assert.NoError(t, err)

	objs, err := r.Query().AddressPlans(context.TODO(), nil, nil)
	assert.NoError(t, err)

	expected := 2
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of address plans")
	assert.Equal(t, ap1, objs[0], "Unexpected address plan")
}

func TestQueryAddressPlansByAddressSpacePlan(t *testing.T) {
	r := newTestPlansResolver(t)
	asp := createAddressSpacePlan("standard", "myaddressspaceplan1", 0, []string{"myaddressplan1", "myaddressplan2"})

	ap1 := createAddressPlan("myaddressplan1", "topic", 0)
	ap2 := createAddressPlan("myaddressplan2", "queue", 1)
	ap3 := createAddressPlan("myaddressplan3", "queue", 2)

	err := r.Cache.Add(asp, ap1, ap2, ap3)
	assert.NoError(t, err)

	objs, err := r.Query().AddressPlans(context.TODO(), &asp.Name, nil)
	assert.NoError(t, err)

	assert.Equal(t, 2, len(objs), "Unexpected number of address plans restricted by space plan")

	topic := AddressTypeTopic
	objs, err = r.Query().AddressPlans(context.TODO(), &asp.Name, &topic)
	assert.NoError(t, err)

	assert.Equal(t, 1, len(objs), "Unexpected number of address plans restricted by space plan and address type")

}

func TestQueryAddressPlansByAddressType(t *testing.T) {
	r := newTestPlansResolver(t)

	ap1 := createAddressPlan("myaddressplan1", "topic", 0)
	ap2 := createAddressPlan("myaddressplan2", "queue", 1)
	ap3 := createAddressPlan("myaddressplan3", "queue", 2)

	err := r.Cache.Add(ap1, ap2, ap3)
	assert.NoError(t, err)

	objs, err := r.Query().AddressPlans(context.TODO(), nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 3, len(objs), "Unexpected number of address plans")

	topic := AddressTypeTopic
	objs, err = r.Query().AddressPlans(context.TODO(), nil, &topic)
	assert.NoError(t, err)

	assert.Equal(t, 1, len(objs), "Unexpected number of address plans restricted by address type")
}

func TestQueryAddressPlan(t *testing.T) {
	r := newTestPlansResolver(t)

	ap := createAddressPlan("myaddressplan1", "queue", 0)

	err := r.Cache.Add(ap)
	assert.NoError(t, err)

	foo := &v1beta1.AddressSpec{
		Plan: "myaddressplan1",
	}
	plan, err := r.AddressSpec_enmasse_io_v1beta1().Plan(context.TODO(), foo)
	assert.NoError(t, err)
	assert.Equal(t, ap, plan, "Unexpected plan")
}

func createAddressSpacePlan(addressSpaceType, addressSpacePlanName string, displayOrder int, addressPlans []string) *v1beta2.AddressSpacePlan {
	asp := &v1beta2.AddressSpacePlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressSpacePlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: addressSpacePlanName,
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressSpacePlanSpec{
			AddressPlans:     addressPlans,
			AddressSpaceType: addressSpaceType,
			DisplayOrder:     displayOrder,
		},
	}
	return asp
}

func createAddressPlan(addressSpaceName, addressType string, displayOrder int) *v1beta2.AddressPlan {
	ap := &v1beta2.AddressPlan{
		TypeMeta: v1.TypeMeta{
			Kind: "AddressPlan",
		},
		ObjectMeta: v1.ObjectMeta{
			Name: addressSpaceName,
			UID:  types.UID(uuid.New().String()),
		},
		Spec: v1beta2.AddressPlanSpec{
			AddressType:  addressType,
			DisplayOrder: displayOrder,
		},
	}
	return ap
}
