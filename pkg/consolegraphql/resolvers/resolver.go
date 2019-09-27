/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

//go:generate go run github.com/99designs/gqlgen generate --config console/console-server/src/main/resources/gqlgen.yml --verbose

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
)

type Resolver struct {
	AdminConfig *v1beta2.AdminV1beta2Client
	CoreConfig  *v1beta1.EnmasseV1beta1Client
	Cache       *cache.MemdbCache
	MetricCache *cache.MemdbCache
}

func (r *Resolver) AddressSpace(ctx context.Context, obj *consolegraphql.ConnectionSpec) (*AddressSpaceConsoleapiEnmasseIoV1beta1, error) {
	panic("implement me")
}

func (r *Resolver) Protocol(ctx context.Context, obj *consolegraphql.ConnectionSpec) (Protocol, error) {
	panic("implement me")
}

func (r *Resolver) Properties(ctx context.Context, obj *consolegraphql.ConnectionSpec) ([]*KeyValue, error) {
	panic("implement me")
}

func (r *Resolver) Query() QueryResolver {
	return &queryResolver{r}
}

func (r *Resolver) Mutation() MutationResolver {
	return &mutationResolver{r}
}

type queryResolver struct{ *Resolver }

type mutationResolver struct{ *Resolver }

func CalcLowerUpper(offset *int, first *int, len int) (int, int) {
	lower := 0
	if offset != nil && *offset > 0 {
		lower = Min(*offset, len)
	}
	upper := len
	if first != nil && *first > 0 {
		upper = Min(lower+*first, len)
	}
	return lower, upper
}

func Min(x, y int) int {
	if x < y {
		return x
	}
	return y
}
