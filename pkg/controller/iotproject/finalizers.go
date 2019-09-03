/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	"fmt"
	"time"

	"github.com/enmasseproject/enmasse/pkg/util/install"

	"sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

var finalizers = []finalizer.Finalizer{
	{
		Name:        "iot.enmasse.io/resources",
		Deconstruct: deconstructResources,
	},
}

func deconstructResources(ctx finalizer.DeconstructorContext) (reconcile.Result, error) {

	project, ok := ctx.Object.(*iotv1alpha1.IoTProject)

	if !ok {
		return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports IoTProject")
	}

	if project.Spec.DownstreamStrategy.ManagedDownstreamStrategy != nil {
		return deconstructManagedResources(project, ctx)
	}

	// nothing to do

	return reconcile.Result{}, nil
}

func deconstructManagedResources(project *iotv1alpha1.IoTProject, ctx finalizer.DeconstructorContext) (reconcile.Result, error) {

	rc := recon.ReconcileContext{}

	rc.Process(func() (result reconcile.Result, e error) {
		return collectResources(ctx.Context, ctx.Client, project, &v1beta1.AddressList{})
	})
	rc.Process(func() (result reconcile.Result, e error) {
		return collectResources(ctx.Context, ctx.Client, project, &v1beta1.AddressSpaceList{})
	})
	rc.Process(func() (result reconcile.Result, e error) {
		return collectResources(ctx.Context, ctx.Client, project, &userv1beta1.MessagingUserList{})
	})

	return rc.Result()

}

// collect all resource which are owned by this project
func collectResources(ctx context.Context, c client.Client, project *iotv1alpha1.IoTProject, list runtime.Object) (reconcile.Result, error) {

	n, err := install.BulkRemoveOwner(ctx, c, project, false, list, client.ListOptions{
		Namespace: project.Namespace,
	})

	if err != nil {
		return reconcile.Result{}, err
	}

	if n > 0 {
		return reconcile.Result{
			RequeueAfter: 10 * time.Second,
		}, nil
	} else {
		return reconcile.Result{}, nil
	}

}
