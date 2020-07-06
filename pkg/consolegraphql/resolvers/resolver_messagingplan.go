/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
)

func (r *queryResolver) MessagingPlans(ctx context.Context, namespace *string) ([]*v1.MessagingPlan, error) {
	requestState := server.GetRequestStateFromContext(ctx)
	viewFilter := requestState.AccessController.ViewFilter()

	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, "MessagingPlan/", viewFilter)
	if e != nil {
		return nil, e
	}
	plans := make([]*v1.MessagingPlan, len(objects))
	for i, o := range objects {
		m, ok := o.(*v1.MessagingPlan)
		if !ok {
			return nil, fmt.Errorf("unexpected type %T", o)
		}
		plans[i] = m
	}
	return plans, e
}
