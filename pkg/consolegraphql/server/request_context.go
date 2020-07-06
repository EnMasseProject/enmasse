/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package server

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/accesscontroller"
	userapiv1 "github.com/openshift/api/user/v1"
)

type RequestState struct {
	EnmasseV1Client  v1.EnmasseV1Interface
	AccessController accesscontroller.AccessController
	User             userapiv1.User
	UserAccessToken  string
	UseSession       bool
	ImpersonatedUser string
}

func ContextWithRequestState(requestState *RequestState, ctx context.Context) context.Context {
	return context.WithValue(ctx, requestStateContextKey, requestState)
}

const requestStateContextKey = "requestStateContextKey"

func GetRequestStateFromContext(ctx context.Context) *RequestState {
	return ctx.Value(requestStateContextKey).(*RequestState)
}
