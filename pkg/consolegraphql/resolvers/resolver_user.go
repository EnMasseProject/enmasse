/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/enmasseproject/enmasse/pkg/util"
	v1 "github.com/openshift/api/user/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (r *queryResolver) Whoami(ctx context.Context) (*v1.User, error) {
	if util.IsOpenshift() {
		requestState := server.GetRequestStateFromContext(ctx)
		return requestState.UserInterface.Get("~", metav1.GetOptions{})
	} else {
		// The Kubernetes server has no API exposing the user's name
		return &v1.User{
			ObjectMeta: metav1.ObjectMeta{
				Name: "unknown",
			},
			FullName:   "unknown",
			Identities: []string{"unknown"},
		}, nil
	}
}
