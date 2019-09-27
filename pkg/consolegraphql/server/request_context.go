/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package server

import (
	"context"
	userv1 "github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
)

const requestStateContextKey = "requestStateContextKey"

type RequestState struct {
	UserInterface userv1.UserInterface
}

func ContextWithRequestState(requestState *RequestState, ctx context.Context) context.Context {
	return context.WithValue(ctx, requestStateContextKey, requestState)
}

func GetRequestStateFromContext(ctx context.Context) *RequestState {
	return ctx.Value(requestStateContextKey).(*RequestState)
}
