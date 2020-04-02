/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	v1 "github.com/openshift/api/user/v1"
)

func (r *queryResolver) Whoami(ctx context.Context) (*v1.User, error) {
	requestState := server.GetRequestStateFromContext(ctx)
	return &requestState.User, nil
}
