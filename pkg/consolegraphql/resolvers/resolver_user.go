/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/enmasseproject/enmasse/pkg/util"
	userv1 "github.com/openshift/api/user/v1"
	authv1 "k8s.io/api/authentication/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (r *queryResolver) Whoami(ctx context.Context) (*userv1.User, error) {
	if util.HasApi(util.UserGVK) {
		requestState := server.GetRequestStateFromContext(ctx)
		return requestState.UserInterface.Get("~", metav1.GetOptions{})
	} else {
		requestState := server.GetRequestStateFromContext(ctx)
		if requestState.ImpersonateUser {
			return &userv1.User{
				ObjectMeta: metav1.ObjectMeta{
					Name: requestState.User,
				},
				FullName:   requestState.User,
				Identities: []string{requestState.User},
			}, nil
		} else {
			username := "unknown"
			req := &authv1.TokenReview{
				Spec: authv1.TokenReviewSpec{
					Token: requestState.UserAccessToken,
				},
			}
			res, err := requestState.AuthenticationInterface.TokenReviews().Create(req)
			if err != nil {
				return nil, err
			} else if res != nil && res.Status.Authenticated {
				username = res.Status.User.Username
			}
			return &userv1.User{
				ObjectMeta: metav1.ObjectMeta{
					Name: username,
				},
				FullName:   username,
				Identities: []string{username},
			}, nil
		}
	}
}
