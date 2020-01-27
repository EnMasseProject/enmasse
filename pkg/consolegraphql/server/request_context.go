/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package server

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/accesscontroller"
	userv1 "github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
)

type RequestState struct {
	UserInterface        userv1.UserInterface
	EnmasseV1beta1Client v1beta1.EnmasseV1beta1Interface
	AccessController     accesscontroller.AccessController
	User                 string
}

func ContextWithRequestState(requestState *RequestState, ctx context.Context) context.Context {
	return context.WithValue(ctx, requestStateContextKey, requestState)
}

const requestStateContextKey = "requestStateContextKey"

func GetRequestStateFromContext(ctx context.Context) *RequestState {
	return ctx.Value(requestStateContextKey).(*RequestState)
}
