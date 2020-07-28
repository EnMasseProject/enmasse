/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

const nameDeviceConnection = "iot-device-connection"

func (r *ReconcileIoTConfig) processDeviceConnection(ctx context.Context, config *iotv1alpha1.IoTConfig, authServicePsk *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	rc := recon.ReconcileContext{}

	// process

	rc.Process(func() (reconcile.Result, error) {
		switch config.EvalDeviceConnectionImplementation() {
		case iotv1alpha1.DeviceConnectionInfinispan:
			return r.processInfinispanDeviceConnection(ctx, config, authServicePsk)
		case iotv1alpha1.DeviceConnectionJdbc:
			return r.processJdbcDeviceConnection(ctx, config, authServicePsk)
		default:
			return reconcile.Result{}, util.NewConfigurationError("illegal device connection configuration")
		}
	})

	// create services

	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameDeviceConnection, config, false, r.reconcileDeviceConnectionService)
	})

	// create metrics service

	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameDeviceConnection+"-metrics", config, false, r.reconcileMetricsService(nameDeviceConnection))
	})

	// done

	return rc.Result()

}

func (r *ReconcileIoTConfig) reconcileDeviceConnectionService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

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

	if err := ApplyInterServiceForService(config, service, nameDeviceConnection); err != nil {
		return err
	}

	return nil
}
