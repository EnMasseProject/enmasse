/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package recon

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util"
	"time"

	"github.com/enmasseproject/enmasse/pkg/util/install"

	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"

	"go.uber.org/multierr"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

type ReconcileContext struct {
	requeue      bool
	requeueAfter time.Duration
	returnNow    bool
	error        error
}

func (r *ReconcileContext) Process(processor func() (reconcile.Result, error)) {
	r.AddResult(processor())
}

func (r *ReconcileContext) ProcessSimple(processor func() error) {
	r.Process(func() (reconcile.Result, error) {
		err := processor()
		return reconcile.Result{}, err
	})
}

func (r *ReconcileContext) Delete(ctx context.Context, client client.Client, object runtime.Object) {
	r.ProcessSimple(func() error {
		return install.DeleteIgnoreNotFound(ctx, client, object)
	})
}

func (r *ReconcileContext) AddResult(result reconcile.Result, err error) {
	if err != nil {
		r.error = multierr.Append(r.error, err)
	} else {
		if result.Requeue {
			r.requeue = true
		}
		if result.RequeueAfter > 0 {
			if r.requeueAfter > 0 {
				r.requeueAfter = util.MinDuration(r.requeueAfter, result.RequeueAfter)
			} else {
				r.requeueAfter = result.RequeueAfter
			}
		}
	}
}

func (r *ReconcileContext) NeedRequeue() bool {
	return r.requeue || r.requeueAfter > 0
}

// Returns the plain result, with no error information.
func (r *ReconcileContext) PlainResult() reconcile.Result {
	return reconcile.Result{
		Requeue:      r.requeue,
		RequeueAfter: r.requeueAfter,
	}
}

func (r *ReconcileContext) Error() error {
	return r.error
}

func (r *ReconcileContext) Result() (reconcile.Result, error) {
	return r.PlainResult(), r.error
}
