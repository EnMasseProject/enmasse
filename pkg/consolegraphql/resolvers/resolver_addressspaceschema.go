/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
)


type addressSpaceSchemaSpecK8sResolver struct{ *Resolver }

func loadAddressSpaceSchemas(r *queryResolver) ([]*v1beta1.AddressSpaceSchema, error) {
	objects, e := r.Cache.Get("hierarchy", "AddressSpaceSchema/", nil)
	if e != nil {
		return nil, e
	}
	ass := make([]*v1beta1.AddressSpaceSchema, len(objects))
	for i, o := range objects {
		ns, ok := o.(*v1beta1.AddressSpaceSchema)
		if !ok {
			return nil, fmt.Errorf("unexpected type %T", o)
		}
		ass[i] = ns
	}

	return ass, e
}

func (r *queryResolver) AddressSpaceSchema(ctx context.Context) ([]*v1beta1.AddressSpaceSchema, error) {

	return loadAddressSpaceSchemas(r)
}

func (r *queryResolver) AddressSpaceSchemaV2(ctx context.Context, addressSpaceType *AddressSpaceType) ([]*v1beta1.AddressSpaceSchema, error) {
	ass,err := loadAddressSpaceSchemas(r)
	if err != nil {
		return nil, err
	}
	if addressSpaceType != nil {
		filtered := make([]*v1beta1.AddressSpaceSchema, 0)
		for _, t := range ass {
			if t.ObjectMeta.Name == string(*addressSpaceType) {
				filtered = append(filtered, t)
			}
		}
		return filtered, nil

	} else {
		return ass, err
	}
}
