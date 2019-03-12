/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package recon

import (
	"context"
	"time"

	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"

	"k8s.io/apimachinery/pkg/api/errors"

	"github.com/enmasseproject/enmasse/pkg/util"
	"go.uber.org/multierr"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

type ReconcileContext struct {
	requeue      bool
	requeueAfter time.Duration
	error        error
}

func (r *ReconcileContext) Process(processor func() (reconcile.Result, error)) {
	result, err := processor()

	if err != nil {
		r.error = multierr.Append(r.error, err)
	} else {
		if result.Requeue {
			r.requeue = true
		}
		r.requeueAfter = util.MaxDuration(r.requeueAfter, result.RequeueAfter)
	}
}

func (r *ReconcileContext) ProcessSimple(processor func() error) {
	r.Process(func() (reconcile.Result, error) {
		err := processor()
		return reconcile.Result{}, err
	})
}

func (r *ReconcileContext) internalDelete(ctx context.Context, client client.Client, object runtime.Object) error {
	err := client.Delete(ctx, object)
	if err == nil || errors.IsNotFound(err) {
		return nil
	} else {
		return err
	}
}

func (r *ReconcileContext) Delete(ctx context.Context, client client.Client, object runtime.Object) {
	r.ProcessSimple(func() error {
		return r.internalDelete(ctx, client, object)
	})
}

func (r *ReconcileContext) NeedRequeue() bool {
	return r.requeue
}

func (r *ReconcileContext) Error() error {
	return r.error
}

func (r *ReconcileContext) Result() (reconcile.Result, error) {
	return reconcile.Result{
		Requeue:      r.requeue,
		RequeueAfter: r.requeueAfter,
	}, r.error
}
