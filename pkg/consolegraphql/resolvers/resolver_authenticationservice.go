/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
)

type authenticationServiceSpecK8sResolver struct{ *Resolver }

func (r *Resolver) AuthenticationServiceSpec_admin_enmasse_io_v1beta1() AuthenticationServiceSpec_admin_enmasse_io_v1beta1Resolver {
	return &authenticationServiceSpecK8sResolver{r}
}

func (r *authenticationServiceSpecK8sResolver) Type(ctx context.Context, obj *v1beta1.AuthenticationServiceSpec) (AuthenticationServiceType, error) {
	return AuthenticationServiceType(obj.Type), nil
}

func (r *queryResolver) AuthenticationServices(ctx context.Context) ([]*v1beta1.AuthenticationService, error) {
	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, "AuthenticationService/", nil)
	if e != nil {
		return nil, e
	}
	nss := make([]*v1beta1.AuthenticationService, len(objects))
	for i, o := range objects {
		ns, ok := o.(*v1beta1.AuthenticationService)
		if !ok {
			return nil, fmt.Errorf("unexpected type %T", o)
		}
		nss[i] = ns
	}
	return nss, e
}
