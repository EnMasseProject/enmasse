/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"

	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (r *ReconcileIoTConfig) processCollector(ctx context.Context, config *iotv1alpha1.IoTConfig) error {

	// we no longer need the "iot-gc", so we only remove it

	d := &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: config.Namespace,
			Name:      "iot-gc",
		},
	}
	return install.DeleteIgnoreNotFound(ctx, r.client, d, client.PropagationPolicy(v1.DeletePropagationForeground))
}
