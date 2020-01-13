/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/99designs/gqlgen/graphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
)

type linkK8sResolver struct{ *Resolver }

type linkSpecK8sResolver struct{ *Resolver }

func (l linkSpecK8sResolver) Connection(ctx context.Context, obj *consolegraphql.LinkSpec) (*consolegraphql.Connection, error) {
	if obj != nil {
		linkrsctx := graphql.GetResolverContext(ctx).Parent.Parent
		link := linkrsctx.Result.(**consolegraphql.Link)

		namespace := (*link).ObjectMeta.Namespace
		objs, e := l.Cache.Get("hierarchy", fmt.Sprintf("Connection/%s/%s/%s", namespace, obj.AddressSpace, obj.Connection), nil)
		if e != nil {
			return nil, e
		}
		if len(objs) == 0 {
			return nil, fmt.Errorf("connection '%s' in address space %s in namespace '%s' not found", obj.Connection, obj.AddressSpace, namespace)
		}

		con := objs[0].(*consolegraphql.Connection)
		return con, nil
	}
	return nil, nil
}

func (l linkSpecK8sResolver) Role(ctx context.Context, obj *consolegraphql.LinkSpec) (LinkRole, error) {
	if obj != nil {
		if obj.Role == "sender" {
			return LinkRoleSender, nil
		} else {
			return LinkRoleReceiver, nil
		}
	}
	return LinkRoleSender, nil
}
