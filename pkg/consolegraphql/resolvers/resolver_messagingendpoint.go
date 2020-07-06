/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"

	"golang.org/x/net/context"
)

func (r *queryResolver) MessagingEndpoints(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*MessagingEndpointQueryResultConsoleapiEnmasseIoV1, error) {
	requestState := server.GetRequestStateFromContext(ctx)
	viewFilter := requestState.AccessController.ViewFilter()

	objects, err := r.Cache.Get(cache.PrimaryObjectIndex, "MessagingEndpoint/", viewFilter)
	if err != nil {
		return nil, err
	}
	endpoints := make([]*v1.MessagingEndpoint, len(objects))
	for i, o := range objects {
		e, ok := o.(*v1.MessagingEndpoint)
		if !ok {
			return nil, fmt.Errorf("unexpected type %T", o)
		}
		endpoints[i] = e
	}

	/*
		err = orderer(endpoints)
		if err != nil {
			return nil, err
		}
	*/

	lower, upper := CalcLowerUpper(offset, first, len(endpoints))
	paged := endpoints[lower:upper]

	mer := &MessagingEndpointQueryResultConsoleapiEnmasseIoV1{
		Total:              len(endpoints),
		MessagingEndpoints: paged,
	}

	return mer, nil
}

func applyFilter(filterFunc cache.ObjectFilter, clusterEndpoint *v1.MessagingEndpoint) (bool, error) {
	if clusterEndpoint == nil {
		return false, nil
	}

	if filterFunc == nil {
		return true, nil
	}

	match, _, err := filterFunc(clusterEndpoint)
	return match, err
}
