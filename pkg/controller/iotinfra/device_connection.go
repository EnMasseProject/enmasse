/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

const nameDeviceConnection = "iot-device-connection"

func (r *ReconcileIoTInfrastructure) processDeviceConnection(ctx context.Context, infra *iotv1.IoTInfrastructure, authServicePsk *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	rc := recon.ReconcileContext{}

	// process

	rc.Process(func() (reconcile.Result, error) {
		switch infra.EvalDeviceConnectionImplementation() {
		case iotv1.DeviceConnectionInfinispan:
			return r.processInfinispanDeviceConnection(ctx, infra, authServicePsk)
		case iotv1.DeviceConnectionJdbc:
			return r.processJdbcDeviceConnection(ctx, infra, authServicePsk)
		default:
			return reconcile.Result{}, util.NewConfigurationError("illegal device connection configuration")
		}
	})

	// create services

	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameDeviceConnection, infra, false, r.reconcileDeviceConnectionService)
	})

	// create metrics service

	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameDeviceConnection+"-metrics", infra, false, r.reconcileMetricsService(nameDeviceConnection))
	})

	// done

	return rc.Result()

}

func (r *ReconcileIoTInfrastructure) reconcileDeviceConnectionService(infra *iotv1.IoTInfrastructure, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", nameDeviceConnection)

	service.Spec.Type = corev1.ServiceTypeClusterIP

	// AMQPS port

	service.Spec.Ports = []corev1.ServicePort{
		{
			Name:       "amqps",
			Port:       5671,
			TargetPort: intstr.FromInt(5671),
			Protocol:   corev1.ProtocolTCP,
		},
	}

	// annotations

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(infra, service, nameDeviceConnection); err != nil {
		return err
	}

	return nil
}
