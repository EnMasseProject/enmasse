/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"k8s.io/api/core/v1"
)

func (r *Resolver) NamespaceStatus_v1() NamespaceStatus_v1Resolver {
	return &namespaceStatusK8sResolver{r}
}

func (r *queryResolver) Namespaces(ctx context.Context) ([]*v1.Namespace, error) {
	objects, e := r.Cache.Get("Namespace/", nil)
	if e != nil {
		return nil, e
	}
	nss := make([]*v1.Namespace, len(objects))
	for i, o := range objects {
		ns, ok := o.(*v1.Namespace)
		if !ok {
			return nil, fmt.Errorf("unexpected type %T", o)
		}
		nss[i] = ns
	}
	return nss, e
}

type namespaceStatusK8sResolver struct{ *Resolver }

func (n *namespaceStatusK8sResolver) Phase(ctx context.Context, obj *v1.NamespaceStatus) (string, error) {
	if obj != nil {
		return string(obj.Phase), nil
	}
	return "", nil
}
